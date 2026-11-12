package estrella.tienda.service;

import com.cloudinary.Cloudinary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import estrella.tienda.common.PaginatedResponse;
import estrella.tienda.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Cloudinary cloudinary;

    private List<Product> products;

    private static final String CLOUDINARY_FOLDER = "Shop/files";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private synchronized void cargarProductos(List<String> sellers) throws IOException {

        products = new ArrayList<>();

        if (sellers == null || sellers.isEmpty()) {
            sellers = List.of("mercadona", "dia", "consum");
        }

        for (String seller : sellers) {
            products.addAll(leerProductosDesdeCloudinary(seller));
        }
    }

    private List<Product> leerProductosDesdeCloudinary(String seller) throws IOException {

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

        cargarProductos(new ArrayList<>());

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
            throw new IllegalArgumentException(
                    "La página no puede ser negativa."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "El tamaño debe estar entre 1 y 100."
            );
        }

        cargarProductos(sellers);

        List<Product> productosFiltrados = products.stream()

                .filter(product -> name == null || name.isBlank()
                        || (product.getName() != null &&
                        product.getName().toLowerCase()
                                .contains(name.trim().toLowerCase())))

                .filter(product -> minPrice == null ||
                        obtenerPrecio(product.getPrice()) >= minPrice)

                .filter(product -> maxPrice == null ||
                        obtenerPrecio(product.getPrice()) <= maxPrice)

                .filter(product -> quantity == null || quantity.isBlank()
                        || (product.getCuantity() != null &&
                        product.getCuantity().toLowerCase()
                                .contains(quantity.trim().toLowerCase())))

                .collect(java.util.stream.Collectors.toCollection(
                        java.util.ArrayList::new
                ));

        if ("asc".equalsIgnoreCase(sortByPrice)) {

            productosFiltrados.sort(
                    Comparator.comparingDouble(
                            product -> obtenerPrecio(product.getPrice())
                    )
            );

        } else if ("desc".equalsIgnoreCase(sortByPrice)) {

            productosFiltrados.sort(
                    Comparator.comparingDouble(
                            (Product product) ->
                                    obtenerPrecio(product.getPrice())
                    ).reversed()
            );

        } else if (sortByPrice != null && !sortByPrice.isBlank()) {

            throw new IllegalArgumentException(
                    "Orden no válido. Utiliza 'asc' o 'desc'."
            );
        }

        return PaginatedResponse.of(productosFiltrados, page, size);
    }

    private double obtenerPrecio(String precio) {

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

    public List<Product> getBySeller(String sellerId) throws IOException {
        try {
            return leerProductosDesdeCloudinary(sellerId);
        } catch (IOException e) {
            return List.of();
        }
    }
}