package estrella.tienda.service;

import estrella.tienda.model.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class CarrefourScrapService {

    private static final String URL_CATEGORIES =
            "https://www.carrefour.es/cloud-api/categories-api/v1/categories/menu/";

    private static final String URL_PRODUCTS =
            "https://www.carrefour.es/cloud-api/plp-food-papi/v1";

    private static final String BASE_URL =
            "https://www.carrefour.es";

    private static final int PAGE_SIZE = 24;

    @Value("${CARREFOUR_CF_BM:}")
    private String cfBm;

    @Value("${CARREFOUR_SESSION_ID:}")
    private String sessionId;


    private final ObjectMapper mapper = new ObjectMapper();

    public void probarCarrefour() {

        try {
            Connection.Response response = Jsoup.connect(
                            "https://www.carrefour.es/"
                    )
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36"
                    )
                    .timeout(30000)
                    .execute();

            System.out.println("HTTP: " + response.statusCode());
            System.out.println("URL: " + response.url());
            System.out.println("Título: " + response.parse().title());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private JsonNode peticionJson(String url) throws IOException {

        Connection connection = Jsoup.connect(url)
                .method(Connection.Method.GET)
                .userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/131.0.0.0 Safari/537.36"
                )
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Referer", "https://www.carrefour.es/")
                .header("Origin", "https://www.carrefour.es")
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .timeout(30000);

        if (!cfBm.isBlank()) {
            connection.cookie("__cf_bm", cfBm);
        }

        if (!sessionId.isBlank()) {
            connection.cookie("session_id", sessionId);
        }

        Connection.Response response = connection.execute();

        int status = response.statusCode();

        System.out.println("Carrefour HTTP: " + status);

        if (status != 200) {
            throw new IOException(
                    "Error Carrefour HTTP " + status +
                            " - Respuesta: " +
                            response.body().substring(
                                    0, Math.min(response.body().length(), 500)
                            )
            );
        }

        return mapper.readTree(response.body());
    }


    /**
     * Obtiene todas las categorías de supermercado.
     */
    public List<String> obtenerCategorias() throws IOException {

        JsonNode response = peticionJson(URL_CATEGORIES);

        Set<String> categorias = new LinkedHashSet<>();

        recorrerCategorias(response.path("menu"), categorias);

        return new ArrayList<>(categorias);
    }

    /**
     * Recorre recursivamente el árbol de categorías.
     */
    private void recorrerCategorias(
            JsonNode nodos,
            Set<String> categorias) {

        if (!nodos.isArray()) {
            return;
        }

        for (JsonNode nodo : nodos) {

            String url = nodo.path("url_rel").asText("");

            JsonNode hijos = nodo.path("childs");

            if (url.startsWith("/supermercado")
                    && !url.contains("ofertas")) {

                if (!hijos.isArray() || hijos.isEmpty()) {
                    categorias.add(url);
                }
            }

            recorrerCategorias(hijos, categorias);
        }
    }

    /**
     * Obtiene todos los productos de todas las categorías.
     */
    public List<Product> obtenerTodosLosProductos()
            throws IOException {

        List<String> categorias = obtenerCategorias();

        Map<String, Product> productos = new LinkedHashMap<>();

        System.out.println(
                "Categorías Carrefour encontradas: " + categorias.size()
        );

        for (String categoria : categorias) {

            System.out.println("Procesando: " + categoria);

            obtenerProductosCategoria(categoria, productos);
        }

        return new ArrayList<>(productos.values());
    }

    /**
     * Obtiene los productos de una categoría mediante offset.
     */
    private void obtenerProductosCategoria(
            String categoria,
            Map<String, Product> productos) throws IOException {

        int offset = 0;

        while (true) {

            String url = URL_PRODUCTS + categoria
                    + "?offset=" + offset;

            JsonNode response;

            try {
                response = peticionJson(url);
            } catch (IOException e) {
                System.err.println(
                        "Error obteniendo categoría " + categoria
                                + ": " + e.getMessage()
                );
                break;
            }

            JsonNode items = response
                    .path("results")
                    .path("items");

            if (!items.isArray() || items.isEmpty()) {
                break;
            }

            for (JsonNode item : items) {

                Product producto = convertirProducto(item);

                if (producto != null) {

                    String clave = producto.getName()
                            + "_" + producto.getPrice();

                    productos.putIfAbsent(clave, producto);
                }
            }

            if (items.size() < PAGE_SIZE) {
                break;
            }

            offset += PAGE_SIZE;
        }
    }

    /**
     * Convierte el JSON de Carrefour a nuestro modelo Product.
     */
    private Product convertirProducto(JsonNode item) {

        String nombre = item.path("name").asText("");

        if (nombre.isBlank()) {
            return null;
        }

        String precio = item.path("price").asText("");

        String cantidad = item.path("measure_unit").asText("");

        String imagen = item.path("images")
                .path("desktop").asText("");

        String url = item.path("url").asText("");

        if (url.startsWith("/")) {
            url = BASE_URL + url;
        }

        return new Product(
                nombre,
                precio,
                cantidad,
                imagen,
                url,
                "Carrefour"
        );
    }

}
