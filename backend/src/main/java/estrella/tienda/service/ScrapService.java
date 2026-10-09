
package estrella.tienda.service;

import estrella.tienda.model.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScrapService {

    private static final String URL =
            "https://books.toscrape.com/catalogue/page-1.html";

    public List<Product> getProducts() throws IOException {

        Document document = Jsoup.connect(URL)
                .userAgent("ProyectoDocker/1.0")
                .timeout(10000)
                .get();

        Elements elements =
                document.select("article.product_pod");

        List<Product> products = new ArrayList<>();

        for (Element element : elements) {

            String name = element
                    .select("h3 a")
                    .attr("title");

            String price = element
                    .select(".price_color")
                    .text();

            String availability = element
                    .select(".availability")
                    .text()
                    .trim();

            Element starRating = element.selectFirst(".star-rating");

            String valoracion = "Sin valoración";

            if (starRating != null) {
                for (String clase : starRating.classNames()) {
                    if (!clase.equals("star-rating")) {
                        valoracion = clase;
                        break;
                    }
                }
            }

            Element elementImg =
                    element.selectFirst(".image_container img");

            String image = elementImg != null
                    ? elementImg.absUrl("src")
                    : "";

            Element elementUrl = element.selectFirst("h3 a");

            String url = elementUrl != null
                    ? elementUrl.absUrl("href")
                    : "";


            Product product = new Product(
                    name,
                    price,
                    availability,
                    image,
                    url
            );

            products.add(product);

            System.out.println("Product: " + name);
            System.out.println("Precio: " + price);
            System.out.println("Disponibilidad: " + availability);
            System.out.println("Valoración: " + valoracion);
            System.out.println("Imagen: " + image);
            System.out.println("---------------------------");
        }

        saveProducstJson(products);

        return products;
    }


    public void saveProducstJson(List<Product> productos)
            throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Path directorio = Paths.get("datos");
        Files.createDirectories(directorio);

        Path archivo = directorio.resolve("productos.json");

        mapper.writeValue(archivo.toFile(), productos);

        System.out.println(
                "JSON guardado en: " +
                        archivo.toAbsolutePath()
        );
    }

}
