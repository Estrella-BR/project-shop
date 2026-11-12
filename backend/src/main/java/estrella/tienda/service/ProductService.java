package estrella.tienda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import estrella.tienda.common.PaginatedResponse;
import estrella.tienda.model.Product;

import com.fasterxml.jackson.core.type.TypeReference;

import org.aspectj.weaver.ast.Literal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ObjectMapper objectMapper;

    private List<Product> products;

    private static final Path FILES_DIR =
            Paths.get("data", "files");


    private synchronized void cargarProductos(List<String> sellers) throws IOException {

        products = new ArrayList<>();

        if (!Files.exists(FILES_DIR)) {
            throw new IOException(
                    "No se encuentra el archivo: " +
                            FILES_DIR.toAbsolutePath()
            );
        }

        //If we don't expecify one we read all
        if( sellers == null || sellers.isEmpty()){
            sellers = new ArrayList<>();
            sellers.add("mercadona");
            sellers.add("dia");
            sellers.add("consum");
        }

        for(String seller: sellers){
            Path filePath = Paths.get("data", "files", seller + ".json");

            products.addAll(objectMapper.readValue(
                    filePath.toFile(),
                    new TypeReference<List<Product>>() {}
            ));
        }
    }

    public PaginatedResponse<Product> getAll(
            int page,
            int size
    ) throws IOException {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "La página no puede ser negativa."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "El tamaño debe estar entre 1 y 100."
            );
        }

        cargarProductos(new ArrayList<>());

        PaginatedResponse<Product> respuesta =
                PaginatedResponse.of(products, page, size);

        return respuesta;
    }

    public PaginatedResponse<Product> getProductsFiltered(
            int page,
            int size,
            String name,
            Double minPrice,
            Double maxPrice,
            String quantity,
            List<String> sellers,
            String sortByPrice
    ) throws IOException {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "La página no puede ser negativa."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "El tamaño debe estar entre 1 y 100."
            );
        }

        cargarProductos(sellers);

        // Filters
        List<Product> productosFiltrados = products.stream()

                .filter(product -> name == null || name.isBlank()
                        || (product.getName() != null &&
                        product.getName().toLowerCase()
                                .contains(name.trim().toLowerCase())))

                .filter(product -> minPrice == null ||
                        obtenerPrecio(product.getPrice()) >= minPrice)

                .filter(product -> maxPrice == null ||
                        obtenerPrecio(product.getPrice()) <= maxPrice)

                .filter(product -> quantity == null || quantity.isBlank()
                        || (product.getCuantity() != null &&
                        product.getCuantity().toLowerCase()
                                .contains(quantity.trim().toLowerCase())))

                .collect(java.util.stream.Collectors.toCollection(
                        java.util.ArrayList::new
                ));

        if ("asc".equalsIgnoreCase(sortByPrice)) {

            productosFiltrados.sort(
                    Comparator.comparingDouble(
                            product -> obtenerPrecio(product.getPrice())
                    )
            );

        } else if ("desc".equalsIgnoreCase(sortByPrice)) {

            productosFiltrados.sort(
                    Comparator.comparingDouble(
                            (Product product) ->
                                    obtenerPrecio(product.getPrice())
                    ).reversed()
            );

        } else if (sortByPrice != null && !sortByPrice.isBlank()) {

            throw new IllegalArgumentException(
                    "Orden no válido. Utiliza 'asc' o 'desc'."
            );
        }

        // Paginar resultados después de ordenar
        PaginatedResponse<Product> respuesta =
                PaginatedResponse.of(productosFiltrados, page, size);

        return respuesta;
    }

    private double obtenerPrecio(String precio) {

        if (precio == null || precio.isBlank()) {
            return 0.0;
        }

        try {
            String valor = precio
                    .replace("€", "")
                    .replace("£", "")
                    .replace("$", "")
                    .trim()
                    .replace(",", ".");

            return Double.parseDouble(valor);

        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public List<Product> getBySeller(String sellerId) throws IOException {

        Path file = FILES_DIR.resolve(sellerId + ".json");

        if (!Files.exists(file)) {
            return List.of();
        }

        return objectMapper.readValue(
                file.toFile(),
                new TypeReference<List<Product>>() {}
        );
    }

}
