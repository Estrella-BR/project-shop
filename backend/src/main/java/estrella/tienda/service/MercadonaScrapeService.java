
package estrella.tienda.service;

import estrella.tienda.model.Product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
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

    // --------------------------------------------------
    // CAMBIAR CODIGO POSTAL
    // --------------------------------------------------

    public String cambiarCodigoPostal(String codigoPostal) {

        try {

            if (codigoPostal == null ||
                    !codigoPostal.matches("\\d{5}")) {

                System.out.println("Código postal no válido.");
                return null;
            }

            // Inicializar sesión y obtener cookies
            Connection.Response initialResponse = session
                    .newRequest(URL + "/")
                    .method(Connection.Method.GET)
                    .execute();


            // Preparar JSON
            String payload =
                    "{\"new_postal_code\":\"" +
                            codigoPostal +
                            "\"}";

            // Enviar petición POST
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

    // --------------------------------------------------
    // OBTENER CATEGORIAS Y SUBCATEGORIAS
    // --------------------------------------------------

    public List<JsonNode> obtenerCategorias(String warehouse) {

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

    // --------------------------------------------------
    // OBTENER PRODUCTOS DE UNA CATEGORIA
    // --------------------------------------------------

    public List<Product> obtenerProductosCategoria(
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

            extraerProductosJson(root, products);

            // Si no se encuentran productos, mostrar la respuesta
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

    // --------------------------------------------------
    // EXTRAER PRODUCTOS DEL JSON
    // --------------------------------------------------

    private void extraerProductosJson(
            JsonNode node,
            List<Product> products
    ) {

        if (node == null) {
            return;
        }

        if (node.isArray()) {

            for (JsonNode child : node) {
                extraerProductosJson(child, products);
            }

            return;
        }

        if (node.isObject()) {

            String name = obtenerTexto(
                    node,
                    "display_name",
                    "name"
            );

            String price = obtenerTexto(
                    node,
                    "price_instructions.unit_price",
                    "price"
            );

            String quantity = obtenerTexto(
                    node,
                    "packaging",
                    "unit_size",
                    "size_format",
                    "format"
            );

            String image = obtenerTexto(
                    node,
                    "thumbnail",
                    "thumbnail_url",
                    "image",
                    "image_url"
            );

            // Reconocer un producto mediante sus campos
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

            // Continuar recorriendo los objetos internos
            node.fields().forEachRemaining(entry -> {

                JsonNode child = entry.getValue();

                if (child.isArray() || child.isObject()) {

                    extraerProductosJson(child, products);
                }
            });
        }
    }

    // --------------------------------------------------
    // OBTENER TEXTO DE UN CAMPO JSON
    // --------------------------------------------------

    private String obtenerTexto(
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

            // Algunos campos de imagen pueden ser objetos JSON
            if (value.isObject()) {

                JsonNode url = value.get("url");

                if (url != null && !url.isNull()) {
                    return url.asText();
                }
            }
        }

        return "";
    }

    // --------------------------------------------------
    // OBTENER PRODUCTOS DE TODAS LAS CATEGORIAS
    // --------------------------------------------------

    public List<Product> extractFromAllCategories() {

        List<Product> products = new ArrayList<>();

        // 1. Configurar código postal
        String warehouse = cambiarCodigoPostal(CODIGO_POSTAL);

        if (warehouse == null || warehouse.isBlank()) {

            System.out.println(
                    "No se ha podido configurar el código postal."
            );

            return products;
        }

        // 2. Obtener subcategorías
        List<JsonNode> categorias = obtenerCategorias(warehouse);

        if (categorias.isEmpty()) {

            System.out.println(
                    "No se han encontrado categorías."
            );

            return products;
        }

        // 3. Recorrer todas las categorías
        Set<String> productosDuplicados = new HashSet<>();

        for (JsonNode categoria : categorias) {

            int categoryId = categoria.path("id").asInt();

            String categoryName =
                    categoria.path("name").asText();


            List<Product> categoryProducts =
                    obtenerProductosCategoria(
                            categoryId,
                            warehouse
                    );

            // Evitar duplicados y procesar solamente productos nuevos
            for (Product product : categoryProducts) {

                String identificador =
                        product.getName() + "_" +
                                product.getPrice();

                if (productosDuplicados.add(identificador)) {

                    // Añadir producto
                    products.add(product);
                }
            }
        }



        return products;
    }
}
