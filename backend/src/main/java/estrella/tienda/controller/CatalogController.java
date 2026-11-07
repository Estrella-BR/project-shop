package estrella.tienda.controller;

import estrella.tienda.common.PaginatedResponse;
import estrella.tienda.model.Product;
import estrella.tienda.service.ProductService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/catalog")
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



    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<Product>> getProductsFiltered(

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,

            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String quantity,
            @RequestParam(required = false) String seller,
            @RequestParam(required = false) String sortByPrice

    ) throws IOException {

        return ResponseEntity.ok(
                productCatalogService.getProductsFiltered(
                        page,
                        size,
                        name,
                        minPrice,
                        maxPrice,
                        quantity,
                        seller,
                        sortByPrice
                )
        );
    }

}
