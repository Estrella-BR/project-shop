package estrella.tienda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import estrella.tienda.common.PaginatedResponse;
import estrella.tienda.model.Product;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
            String nombre,
            Double precioMin,
            Double precioMax,
            String cantidad,
            String vendedor
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

        List<Product> productosFiltrados = products.stream()

                // Filtrar por nombre
                .filter(product -> nombre == null || nombre.isBlank()
                        || (product.getName() != null &&
                        product.getName().toLowerCase()
                                .contains(nombre.trim().toLowerCase())))

                // Filtrar por precio mínimo
                .filter(product -> precioMin == null ||
                        obtenerPrecio(product.getPrice()) >= precioMin)

                // Filtrar por precio máximo
                .filter(product -> precioMax == null ||
                        obtenerPrecio(product.getPrice()) <= precioMax)

                // Filtrar por cantidad
                .filter(product -> cantidad == null || cantidad.isBlank()
                        || (product.getCuantity() != null &&
                        product.getCuantity().toLowerCase()
                                .contains(cantidad.trim().toLowerCase())))

                // Filtrar por vendedor
                .filter(product -> vendedor == null || vendedor.isBlank()
                        || (product.getSeller() != null &&
                        product.getSeller().toLowerCase()
                                .contains(vendedor.trim().toLowerCase())))

                .toList();

        // Paginar resultados
        PaginatedResponse<Product> respuesta =
                PaginatedResponse.of(productosFiltrados, page, size);

        // Procesar imágenes
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
