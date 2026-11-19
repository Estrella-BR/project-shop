package estrella.tienda.service;

import com.cloudinary.Cloudinary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import estrella.tienda.common.PaginatedResponse;
import estrella.tienda.model.Product;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Cloudinary cloudinary;

    private List<Product> products;

    private static final String CLOUDINARY_FOLDER = "Shop/files";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final JaroWinklerSimilarity JW = new JaroWinklerSimilarity();
    private static final double NAME_THRESHOLD = 0.85;

    private synchronized void loadProducts(List<String> sellers) throws IOException {

        products = new ArrayList<>();

        if (sellers == null || sellers.isEmpty()) {
            sellers = List.of("mercadona", "dia", "consum");
        }

        for (String seller : sellers) {
            products.addAll(getProductsFromCloudinary(seller));
        }
    }

    private List<Product> getProductsFromCloudinary(String seller) throws IOException {

        String publicId = CLOUDINARY_FOLDER + "/" + seller;

        String url = cloudinary.url()
                .resourceType("raw")
                .secure(true)
                .generate(publicId);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() != 200) {
                throw new IOException(
                        "No se pudo descargar " + publicId +
                                " desde Cloudinary. HTTP " + response.statusCode()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<Product>>() {}
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Descarga interrumpida: " + publicId, e);
        }
    }

    public PaginatedResponse<Product> getAll(
            int page,
            int size
    ) throws IOException {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "La página no puede ser negativa."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "El tamaño debe estar entre 1 y 100."
            );
        }

        loadProducts(new ArrayList<>());

        return PaginatedResponse.of(products, page, size);
    }

    public PaginatedResponse<Product> getProductsFiltered(
            int page,
            int size,
            String name,
            Double minPrice,
            Double maxPrice,
            String quantity,
            List<String> sellers,
            String sortByPrice
    ) throws IOException {

        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100.");
        }

        loadProducts(sellers);

        final String nameQuery = (name != null && !name.isBlank()) ? name.trim() : null;
        final String quantityQuery = (quantity != null && !quantity.isBlank())
                ? toPlainText(quantity) : null;

        // Product + name similarity score pair
        record Scored(Product p, double score) {}

        List<Scored> scored = new ArrayList<>();

        for (Product product : products) {

            if (minPrice != null && getPrice(product.getPrice()) < minPrice) continue;
            if (maxPrice != null && getPrice(product.getPrice()) > maxPrice) continue;

            if (quantityQuery != null) {
                if (product.getQuantity() == null) continue;
                if (!toPlainText(product.getQuantity()).contains(quantityQuery)) continue;
            }

            double score = 1.0;
            if (nameQuery != null) {
                score = nameSimilarity(nameQuery, product.getName());
                if (score < NAME_THRESHOLD) continue;
            }

            scored.add(new Scored(product, score));
        }

        if ("asc".equalsIgnoreCase(sortByPrice)) {
            scored.sort(Comparator.comparingDouble(
                    (Scored s) -> getPrice(s.p().getPrice())));
        } else if ("desc".equalsIgnoreCase(sortByPrice)) {
            scored.sort(Comparator.comparingDouble(
                    (Scored s) -> getPrice(s.p().getPrice())).reversed());
        } else if (sortByPrice != null && !sortByPrice.isBlank()) {
            throw new IllegalArgumentException("Invalid sort order. Use 'asc' or 'desc'.");
        } else if (nameQuery != null) {
            // order by relevance
            scored.sort(Comparator.comparingDouble(
                    (Scored s) -> s.score()).reversed());
        }

        List<Product> filtered = scored.stream()
                .map(Scored::p)
                .collect(Collectors.toList());

        return PaginatedResponse.of(filtered, page, size);
    }

    private double getPrice(String precio) {

        if (precio == null || precio.isBlank()) {
            return 0.0;
        }

        try {
            String valor = precio
                    .replace("€", "")
                    .replace("£", "")
                    .replace("$", "")
                    .trim()
                    .replace(",", ".");

            return Double.parseDouble(valor);

        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String toPlainText(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return t;
    }

    private double nameSimilarity(String query, String productName) {
        String q = toPlainText(query);
        String n = toPlainText(productName);

        if (q.isEmpty()) return 1.0;
        if (n.isEmpty()) return 0.0;

        // Direct hit → top priority
        if (n.contains(q)) return 1.0;

        // Jaro-Winkler over the whole string (tolerates typos)
        double base = JW.apply(q, n);

        // And word-by-word (in case the user reorders terms)
        double byWords = 0.0;
        for (String token : q.split(" ")) {
            if (token.length() < 2) continue;
            for (String word : n.split(" ")) {
                byWords = Math.max(byWords, JW.apply(token, word));
            }
        }

        return Math.max(base, byWords);
    }
}