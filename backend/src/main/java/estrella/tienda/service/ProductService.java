package estrella.tienda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import estrella.tienda.common.PaginatedResponse;
import estrella.tienda.model.Product;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    @Autowired
    private  ImageProcessingService imageProcessingService;


    private List<Product> products;

    private static final Path PRODUCTOS_PATH =
            Paths.get("datos", "productos.json");


    // Leer el JSON una sola vez y mantenerlo en memoria
    private synchronized void cargarProductos() throws IOException {

        if (products != null) {
            return;
        }

        if (!Files.exists(PRODUCTOS_PATH)) {
            throw new IOException(
                    "No se encuentra el archivo: " +
                            PRODUCTOS_PATH.toAbsolutePath()
            );
        }

        products = objectMapper.readValue(
                PRODUCTOS_PATH.toFile(),
                new TypeReference<List<Product>>() {}
        );

        System.out.println(
                "Productos cargados en memoria: " +
                        products.size()
        );
    }

    // Obtener productos paginados
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

        cargarProductos();

        // Obtener productos paginados
        PaginatedResponse<Product> respuesta =
                PaginatedResponse.of(products, page, size);

        // Procesar imágenes de los productos de la página actual
        for (Product product : respuesta.content()) {
            if (product.getCleanImage() == null) {
                product.setCleanImage(
                        imageProcessingService.processImage(
                                product.getImage()
                        )
                );
            }
        }

        return respuesta;
    }

    public PaginatedResponse<Product> getProductsFiltered(
            int page,
            int size,
            String name,
            Double minPrice,
            Double maxPrice,
            String quantity,
            String seller,
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

        cargarProductos();

        // Aplicar filtros
        List<Product> productosFiltrados = products.stream()

                // Filtrar por nombre
                .filter(product -> name == null || name.isBlank()
                        || (product.getName() != null &&
                        product.getName().toLowerCase()
                                .contains(name.trim().toLowerCase())))

                // Filtrar por precio mínimo
                .filter(product -> minPrice == null ||
                        obtenerPrecio(product.getPrice()) >= minPrice)

                // Filtrar por precio máximo
                .filter(product -> maxPrice == null ||
                        obtenerPrecio(product.getPrice()) <= maxPrice)

                // Filtrar por cantidad
                .filter(product -> quantity == null || quantity.isBlank()
                        || (product.getCuantity() != null &&
                        product.getCuantity().toLowerCase()
                                .contains(quantity.trim().toLowerCase())))

                // Filtrar por vendedor
                .filter(product -> seller == null || seller.isBlank()
                        || (product.getSeller() != null &&
                        product.getSeller().toLowerCase()
                                .contains(seller.trim().toLowerCase())))

                .collect(java.util.stream.Collectors.toCollection(
                        java.util.ArrayList::new
                ));

        // Ordenar por precio
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

        // Procesar imágenes de la página actual
        for (Product product : respuesta.content()) {
            if (product.getCleanImage() == null) {
                product.setCleanImage(
                        imageProcessingService.processImage(
                                product.getImage(),product.getImgName()
                        )
                );
            }
        }

        return respuesta;
    }

    // Convertir precio String a Double
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

}
