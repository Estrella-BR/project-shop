package estrella.tienda.controller;

import estrella.tienda.common.PaginatedResponse;
import estrella.tienda.model.Product;
import estrella.tienda.service.ProductService;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/catalog")
@CrossOrigin(origins = "*")
public class CatalogController {

    private final ProductService productCatalogService;

    public CatalogController(
            ProductService productCatalogService
    ) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping
    public PaginatedResponse<Product> obtenerProductos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws IOException {

        return productCatalogService.obtenerProductos(
                page,
                size
        );
    }
}
