
package estrella.tienda.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
import java.util.Map;

@Service
public class ScrapService {

    @Autowired
    private MercadonaScrapeService mercadonaScrapeService;

    @Autowired
    private BooksToScrapeService booksToScrapeService;

    @Autowired
    private  ImageProcessingService imageProcessingService;

    @Autowired
    private ConsumScrapService consumScrapService;

    @Autowired
    private DiaScrapService diaScrapService;

    @Autowired
    private Cloudinary cloudinary;


    public List<Product> getAllBooks() throws IOException {

        List<Product> products = new ArrayList<>();

        products.addAll(booksToScrapeService.getAllProducts());

        saveProducstJson(products, "libros");

        return products;
    }

    public List<Product> getAllSellers(Boolean cleanImg) throws IOException {

        List<Product> products = new ArrayList<>();

        products.addAll(mercadonaScrapeService.extractFromAllCategories());
        products.addAll(diaScrapService.extractFromAllCategories());
        products.addAll(consumScrapService.extractFromAllCategories());

        cleanImages(cleanImg,products);

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

        byte[] jsonBytes = mapper.writeValueAsBytes(productos);

        Map uploadResult = cloudinary.uploader().upload(
                jsonBytes,
                ObjectUtils.asMap(
                        "resource_type", "raw",
                        "public_id", fileName,
                        "folder", "Shop/files"
                )
        );

         Path directorio = Paths.get("data", "files");
         Files.createDirectories(directorio);
         Path archivo = directorio.resolve(fileName + ".json");
         mapper.writeValue(archivo.toFile(), productos);
    }



    public List<Product> getMercadonaProducts(Boolean cleanImg)throws IOException{
        List<Product> products = new ArrayList<>();

        products.addAll(mercadonaScrapeService.extractFromAllCategories());

        cleanImages(cleanImg,products);

        saveProducstJson(products, "mercadona");

        System.out.println("Mercadona file created.");

        return products;
    }

    public List<Product> getDiaProducts(Boolean cleanImg)throws IOException{
        List<Product> products = new ArrayList<>();

        products.addAll(diaScrapService.extractFromAllCategories());

        cleanImages(cleanImg,products);

        saveProducstJson(products, "dia");

        System.out.println("Dia file created.");

        return products;
    }

    public List<Product> getConsumProducts(Boolean cleanImg)throws IOException{
        List<Product> products = new ArrayList<>();

        products.addAll(consumScrapService.extractFromAllCategories());

        cleanImages(cleanImg,products);

        saveProducstJson(products, "consum");

        System.out.println("Consum file created.");

        return products;
    }

    private void cleanImages(Boolean cleanImg, List<Product> products){
        if(cleanImg == true){
            for( Product prod : products){
                prod.setCleanImage( imageProcessingService.processImage(
                        prod.getImage(),prod.createImgName()
                ));
            }
        }
    }
}
