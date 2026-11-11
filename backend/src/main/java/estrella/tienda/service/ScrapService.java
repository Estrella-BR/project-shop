
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
    private MercadonaScrapeService mercadonaScrapeService;

    @Autowired
    private BooksToScrapeService booksToScrapeService;

    @Autowired
    private ConsumScrapService consumScrapService;

    @Autowired
    private DiaScrapService diaScrapService;


    public List<Product> getAllBooks() throws IOException {

        List<Product> products = new ArrayList<>();

        products.addAll(booksToScrapeService.getAllProducts());

        saveProducstJson(products, "libros");

        return products;
    }

    public List<Product> getAllProducts() throws IOException {

        List<Product> products = new ArrayList<>();

        //Call services
        products.addAll(mercadonaScrapeService.extractFromAllCategories());
       // products.addAll(diaScrapService.extractFromAllCategories());
        //products.addAll(consumScrapService.extractFromAllCategories());

        saveProducstJson(products, "productos");

        System.out.println("File created.");

        return products;
    }

    public void saveProducstJson(
            List<Product> productos,
            String fileName
    ) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Path directorio = Paths.get("datos");

        Files.createDirectories(directorio);

        Path archivo = directorio.resolve(fileName + ".json");

        mapper.writeValue(archivo.toFile(), productos);
    }
}
