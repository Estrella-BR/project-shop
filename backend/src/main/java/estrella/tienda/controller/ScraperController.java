package estrella.tienda.controller;

import estrella.tienda.model.Product;
import estrella.tienda.service.ScrapService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
public class ScraperController {
    private final ScrapService scraperService;

    public ScraperController(ScrapService scraperService) {
        this.scraperService = scraperService;
    }


    @GetMapping("/products")
    public List<Product> getProducts(
            @RequestParam(defaultValue = "false") Boolean cleanImg) {
        try {
            return scraperService.getAllSellers(cleanImg);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se han podido obtener los productos de las tiendas"
            );
        }
    }


    @GetMapping("/products/mercadona")
    public List<Product> getMercadonaProducts(
            @RequestParam(defaultValue = "false") Boolean cleanImg) {
        try {
            return scraperService.getMercadonaProducts(cleanImg);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se han podido obtener los productos de mercadona"
            );
        }
    }

    @GetMapping("/products/dia")
    public List<Product> getDiaProducts(
            @RequestParam(defaultValue = "false") Boolean cleanImg) {
        try {
            return scraperService.getDiaProducts(cleanImg);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se han podido obtener los productos de dia"
            );
        }
    }

    @GetMapping("/products/consum")
    public List<Product> getConsumProducts(
            @RequestParam(defaultValue = "false") Boolean cleanImg) {
        try {
            return scraperService.getConsumProducts(cleanImg);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se han podido obtener los productos de consum"
            );
        }
    }

    @GetMapping("/books")
    public List<Product> getBooks() {
        try {
            return scraperService.getAllBooks();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se han podido obtener los productos de la tienda"
            );
        }
    }
}
