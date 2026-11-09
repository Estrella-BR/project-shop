package estrella.tienda.service;

import estrella.tienda.model.Product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import tools.jackson.databind.node.ArrayNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;

@Service
public class ConsumScrapService {


    private static final int PAGE_SIZE = 24;
    private static final int MAX_PAGINAS_POR_CATEGORIA = 60;

    // Constantes corregidas
    private static final String URL = "https://tienda.consum.es";
    // La base de la API es diferente
    private static final String CATEGORIES_URL = URL + "/api/rest/V1.0/shopping/category/menu";


    private final Connection session;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConsumScrapService() {
        this.session = Jsoup.newSession()
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Origin", URL)
                .header("Referer", URL + "/");
    }

    public List<Product> extractFromAllCategories() {

        List<Product> products = new ArrayList<>();

        for(String category: this.getCategories()){
            products.addAll(this.getProductsFromCategory(category));
        }

        return products;
    }

    private List< Product> getProductsFromCategory(String categoryUrl) {
        List<Product> products = new ArrayList<>();
        try {
            Connection.Response response = session
                    .newRequest(categoryUrl)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .timeout(20_000)
                    .execute();

            if (response.statusCode() != 200) {
                return products;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return products;
    }

    private List<String> getCategories(){

        List<String> categories = new ArrayList<>();

        try {
            Connection.Response response = session
                    .newRequest(CATEGORIES_URL)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            if (response.statusCode() != 200) {
                System.out.println(
                        "Error obteniendo categorías: " +
                                response.body()
                );

            }

            JsonNode root = objectMapper.readTree(response.body());

            for(JsonNode node: root){
                    String url = node.path("url").asText();
                    categories.add(url);
                }

        }
        catch (Exception e){

        }

        return categories;
    }
}