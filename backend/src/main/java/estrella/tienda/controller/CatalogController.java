package estrella.tienda.controller;

import estrella.tienda.common.PaginatedResponse;
import estrella.tienda.model.Product;
import estrella.tienda.service.ProductService;

import org.springframework.http.ResponseEntity;
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

    @RequestMapping(path = "", method = RequestMethod.GET)
    public PaginatedResponse<Product> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws IOException {

        return productCatalogService.getAll(
                page,
                size
        );
    }



    @GetMapping("/productos/filtrar")
    public ResponseEntity<PaginatedResponse<Product>> getProductsFiltered(

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,

            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String cantidad,
            @RequestParam(required = false) String vendedor

    ) throws IOException {

        return ResponseEntity.ok(
                productCatalogService.getProductsFiltered(
                        page,
                        size,
                        nombre,
                        precioMin,
                        precioMax,
                        cantidad,
                        vendedor
                )
        );
    }

}
