package estrella.tienda.controller;

import estrella.tienda.model.Product;
import estrella.tienda.service.ScrapService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/productos")
    public List<Product> getProducts() {
        try {
            return scraperService.getProducts();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se han podido obtener los productos de la tienda"
            );
        }
    }
}
