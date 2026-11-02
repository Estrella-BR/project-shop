package estrella.tienda.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import estrella.tienda.common.PaginatedResponse;
import estrella.tienda.model.Product;

import com.fasterxml.jackson.core.type.TypeReference;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ObjectMapper objectMapper;

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
    public PaginatedResponse<Product> obtenerProductos(
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

        int totalElements = products.size();

        int totalPages = (int) Math.ceil(
                (double) totalElements / size
        );

        int inicio = page * size;

        if (inicio >= totalElements) {

            return new PaginatedResponse<>(
                    Collections.emptyList(),
                    page,
                    size,
                    totalElements,
                    totalPages
            );
        }

        int fin = Math.min(
                inicio + size,
                totalElements
        );

        List<Product> contenido = products.subList(
                inicio,
                fin
        );

        return new PaginatedResponse<>(
                contenido,
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
