
package estrella.tienda.service;

import estrella.tienda.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    MercadonaScrapeService mercadonaScrapeService;

    @Autowired
    BooksToScrapeService booksToScrapeService;


    public List<Product> getAllBooks() throws IOException {
        List<Product> products = new ArrayList<>();

        System.out.println("Empezando a obtener los libros...");

        products.addAll(booksToScrapeService.getAllProducts());

        System.out.println("Productos obtenidos.");


        saveProducstJson(products);

        System.out.println("Archivo creado");


        return products;
    }




    public List<Product> getAllProducts() throws IOException {
        List<Product> products = new ArrayList<>();

        System.out.println("Empezando a obtener los productos...");

        products.addAll(mercadonaScrapeService.extractFromAllCategories());

        System.out.println("Productos obtenidos.");


        saveProducstJson(products);

        System.out.println("Archivo creado");


        return products;
    }


    public void saveProducstJson(List<Product> productos) throws IOException {

        System.out.println("Tamaño de list saveProducstJson: " + productos.size());
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
