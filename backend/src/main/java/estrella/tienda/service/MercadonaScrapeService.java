
package estrella.tienda.service;

import estrella.tienda.model.Product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MercadonaScrapeService {

    private static final String URL = "https://tienda.mercadona.es";

    private static final String CODIGO_POSTAL = "46026";

    private final Connection session;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MercadonaScrapeService() {

        this.session = Jsoup.newSession()
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Origin", URL)
                .header("Referer", URL + "/");
    }


    public String changeCP(String cp) {

        try {

            if (cp == null ||
                    !cp.matches("\\d{5}")) {

                System.out.println("Código postal no válido.");
                return null;
            }

            Connection.Response initialResponse = session
                    .newRequest(URL + "/")
                    .method(Connection.Method.GET)
                    .execute();


            String payload =
                    "{\"new_postal_code\":\"" +
                            cp +
                            "\"}";


            Connection.Response response = session
                    .newRequest(
                            URL +
                                    "/api/postal-codes/actions/change-pc/?lang=es&wh=vlc1"
                    )
                    .method(Connection.Method.POST)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/plain, */*")
                    .requestBody(payload)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            String warehouse = response.header("x-customer-wh");

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                return null;
            }

            return warehouse;

        } catch (Exception e) {

            System.err.println(
                    "Error al cambiar el código postal:"
            );

            e.printStackTrace();

            return null;
        }
    }


    public List<JsonNode> obtainCategories(String warehouse) {

        List<JsonNode> categorias = new ArrayList<>();

        try {

            String url = URL +
                    "/api/categories/?lang=es&wh=" +
                    warehouse;

            Connection.Response response = session
                    .newRequest(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();


            if (response.statusCode() != 200) {
                System.out.println(
                        "Error obteniendo categorías: " +
                                response.body()
                );
                return categorias;
            }

            JsonNode root = objectMapper.readTree(response.body());

            JsonNode results = root.get("results");

            if (results == null || !results.isArray()) {
                System.out.println(
                        "No se ha encontrado la lista de categorías."
                );
                return categorias;
            }

            for (JsonNode categoriaPrincipal : results) {

                JsonNode subcategorias =
                        categoriaPrincipal.get("categories");

                if (subcategorias == null ||
                        !subcategorias.isArray()) {
                    continue;
                }

                for (JsonNode subcategoria : subcategorias) {

                    if (subcategoria.path("published").asBoolean()) {

                        categorias.add(subcategoria);
                    }
                }
            }


        } catch (Exception e) {

            System.err.println(
                    "Error obteniendo las categorías:"
            );

            e.printStackTrace();
        }

        return categorias;
    }

    public List<Product> obtainProductsInCategory(
            int categoryId,
            String warehouse
    ) {

        List<Product> products = new ArrayList<>();

        try {

            String url = URL +
                    "/api/categories/" +
                    categoryId +
                    "/?lang=es&wh=" +
                    warehouse;


            Connection.Response response = session
                    .newRequest(url)
                    .method(Connection.Method.GET)
                    .header("Accept", "application/json, text/plain, */*")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            if (response.statusCode() != 200) {

                System.out.println(
                        "Error HTTP categoría " +
                                categoryId +
                                ": " +
                                response.statusCode()
                );

                System.out.println(response.body());

                return products;
            }

            JsonNode root = objectMapper.readTree(response.body());

            extractProductJson(root, products);

            if (products.isEmpty()) {

                System.out.println(
                        "No se han reconocido productos en la respuesta JSON:"
                );

                System.out.println(
                        response.body().substring(
                                0,
                                Math.min(response.body().length(), 1500)
                        )
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "Error descargando categoría " +
                            categoryId
            );

            e.printStackTrace();
        }

        return products;
    }

    private void extractProductJson(
            JsonNode node,
            List<Product> products
    ) {

        if (node == null) {
            return;
        }

        if (node.isArray()) {

            for (JsonNode child : node) {
                extractProductJson(child, products);
            }

            return;
        }

        if (node.isObject()) {

            String name = obtainText(
                    node,
                    "display_name",
                    "name"
            );

            String price = obtainText(
                    node,
                    "price_instructions.unit_price",
                    "price"
            );

            String quantity = extractQuantity(node);

            String image = obtainText(
                    node,
                    "thumbnail",
                    "thumbnail_url",
                    "image",
                    "image_url"
            );


            boolean esProducto =
                    !name.isEmpty() &&
                            (
                                    node.has("price_instructions") ||
                                            node.has("price")
                            ) &&
                            !image.isEmpty();

            if (esProducto) {

                String productUrl = "";

                Product product = new Product(
                        name,
                        price,
                        quantity,
                        image,
                        productUrl,
                        "Mercadona"
                );
                product.generateId();

                products.add(product);

            }

            node.fields().forEachRemaining(entry -> {

                JsonNode child = entry.getValue();

                if (child.isArray() || child.isObject()) {

                    extractProductJson(child, products);
                }
            });
        }
    }

    private String obtainText(
            JsonNode node,
            String... paths
    ) {

        for (String path : paths) {

            JsonNode value = node;

            String[] parts = path.split("\\.");

            for (String part : parts) {

                if (value == null) {
                    break;
                }

                value = value.get(part);
            }

            if (value == null || value.isNull()) {
                continue;
            }

            if (value.isValueNode()) {

                String result = value.asText().trim();

                if (!result.isEmpty()) {
                    return result;
                }
            }

            if (value.isObject()) {

                JsonNode url = value.get("url");

                if (url != null && !url.isNull()) {
                    return url.asText();
                }
            }
        }

        return "";
    }

    private String extractQuantity(JsonNode node) {
        JsonNode price = node.get("price_instructions");
        if (price == null || !price.isObject()) {
            return "";
        }

        boolean isPack = price.path("is_pack").asBoolean(false);
        int totalUnits = price.path("total_units").asInt(0);
        String unitName = price.path("unit_name").asText("").trim();
        double packSize = price.path("pack_size").asDouble(0);
        double unitSize = price.path("unit_size").asDouble(0);
        String sizeFormat = price.path("size_format").asText("").trim();
        String packaging = node.path("packaging").asText("").trim();

        if (isPack && totalUnits > 0 && packSize > 0) {
            String weight = formatWeight(packSize, sizeFormat);
            if (!weight.isEmpty()) {
                if (!unitName.isEmpty()) {
                    return totalUnits + " " + unitName + " x " + weight;
                }
                return totalUnits + " x " + weight;
            }
        }

        if (unitSize > 0) {
            String weight = formatWeight(unitSize, sizeFormat);
            if (!weight.isEmpty()) {
                if (!packaging.isEmpty()) {
                    return packaging + " " + weight;
                }
                return weight;
            }
        }

        return "";
    }

    private String formatWeight(double size, String format) {
        if (size <= 0 || format.isEmpty()) {
            return "";
        }

        switch (format.toLowerCase()) {
            case "kg":
                return size < 1
                        ? formatNumber(size * 1000) + " g"
                        : formatNumber(size) + " kg";
            case "g":
                return size >= 1000
                        ? formatNumber(size / 1000) + " kg"
                        : formatNumber(size) + " g";
            case "l":
                return size < 1
                        ? formatNumber(size * 1000) + " ml"
                        : formatNumber(size) + " L";
            case "ml":
                return size >= 1000
                        ? formatNumber(size / 1000) + " L"
                        : formatNumber(size) + " ml";
            default:
                return formatNumber(size) + " " + format;
        }
    }

    private String formatNumber(double value) {
        long rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.001) {
            return String.valueOf(rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    public List<Product> extractFromAllCategories() {

        List<Product> products = new ArrayList<>();

        String warehouse = changeCP(CODIGO_POSTAL);

        if (warehouse == null || warehouse.isBlank()) {

            System.out.println(
                    "No se ha podido configurar el código postal."
            );

            return products;
        }

        List<JsonNode> categorias = obtainCategories(warehouse);

        if (categorias.isEmpty()) {

            System.out.println(
                    "No se han encontrado categorías."
            );

            return products;
        }

        Set<String> productosDuplicados = new HashSet<>();

        for (JsonNode categoria : categorias) {

            int categoryId = categoria.path("id").asInt();

            String categoryName =
                    categoria.path("name").asText();

            List<Product> categoryProducts =
                    obtainProductsInCategory(
                            categoryId,
                            warehouse
                    );
            //Check for duplicates
            for (Product product : categoryProducts) {

                String identificador =
                        product.getName() + "_" +
                                product.getPrice();

                if (productosDuplicados.add(identificador)) {
                    products.add(product);
                }
            }
        }



        return products;
    }
}
