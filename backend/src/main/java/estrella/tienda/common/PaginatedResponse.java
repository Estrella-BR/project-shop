package estrella.tienda.common;

import java.util.Collections;
import java.util.List;

public record PaginatedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PaginatedResponse<T> of(
            List<T> elements,
            int page,
            int size
    ) {
        int totalElements = elements.size();

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

        int fin = Math.min(inicio + size, totalElements);

        List<T> contenido = elements.subList(inicio, fin);

        return new PaginatedResponse<>(
                contenido,
                page,
                size,
                totalElements,
                totalPages
        );
    }
}