package estrella.tienda.service;

import estrella.tienda.model.Product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConsumScrapService {
    private static final String URL = "https://tienda.consum.es/api/rest/V1.0";
    private static final String CATEGORIES_URL = URL + "/shopping/category/menu";
    private static final String PRODUCTS_URL = URL + "/catalog/product";
    private static final int maxTry = 50;
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

    private List<Product> getProductsFromCategory(String categoryUrl) {
        List<Product> products = new ArrayList<>();
        int tries = 0;
        int limit = 50;
        boolean hasMore = true;

        //Change the limit until all the products are on one page
        while(hasMore == true && maxTry > tries) {
            try {
                Connection.Response response = session
                        .newRequest( PRODUCTS_URL)
                        .method(Connection.Method.GET)
                        .data("limit", String.valueOf(limit))
                        .data("categories", categoryUrl)
                        .data("page", "1")
                        .data("orderById", "5")
                        .data("showProducts", "true")
                        .data("showRecommendations", "false")
                        .data("includeFilters", "false")
                        .data("originProduct", "undefined")
                        .data("offset", "1")
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .execute();
                tries++;
                limit = limit + 25;

                if (response.statusCode() != 200) {
                    System.out.println(
                            "Error obtaining products: " +
                                    response.body() + " " + URL + PRODUCTS_URL
                    );
                }

                JsonNode root = objectMapper.readTree(response.body());

                boolean moreObjects = root.path("hasMore").asBoolean(true);

                if (moreObjects == true) {
                    hasMore = false;

                    JsonNode items = root.path("products");

                    for(JsonNode node: items){
                        products.add(this.getProductFromJSON(node));
                    }

                }
            } catch (Exception e){
                hasMore = false;
                System.out.println("Error durante la conexión: " + e);
            }
        }

        return products;
    }

    private Product getProductFromJSON(JsonNode node){
        Product p = new Product();

        p.setName(node.path("productData").path("name").asText(null));
        p.setUrl(node.path("productData").path("url").asText(null));
        p.setImage(node.path("media").path(0).path("url").asText(null));
        String price = node.path("priceData").path("prices").path(0).path("value").path("centAmount").asText();
        String description = node.path("productData").path("description").asText(null);

        p.setCuantity(this.getQuantity(description));
        p.setPrice(this.cleanPrice(price));
        p.setSeller("Consum");
        return p;
    }

    private String cleanPrice(String ogPrice){
        String clean = ogPrice.replaceAll("0+$", "").replaceAll("\\.$", "");
        return clean;
    }

    private String getQuantity(String description){
        Pattern pattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(Gr|gr|g|G|Kg|kg|Ml|ml|L|l)\\b");
        Matcher matcher = pattern.matcher(description);

        String cuantiy = "";

        if (matcher.find()) {
            cuantiy = matcher.group(0);
        }

        return cuantiy;
    }

    private List<String> getCategories(){
        List<String> categories = new ArrayList<>();
        try {
            Connection.Response response = session
                    .newRequest( CATEGORIES_URL)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            if (response.statusCode() != 200) {
                System.out.println(
                        "Error obtaining categories: " +
                                response.body()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());

            for(JsonNode node: root){
                    String id = node.path("id").asText();
                    categories.add(id);
                }

        }
        catch (Exception e){
            System.out.println("Error getting the categories: " + e);
        }

        return categories;
    }
}