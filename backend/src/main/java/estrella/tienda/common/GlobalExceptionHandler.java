package estrella.tienda.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(
            IllegalArgumentException e
    ) {
        return ResponseEntity.badRequest().body(
                Map.of(
                        "ok", false,
                        "error", e.getMessage()
                )
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handlePythonError(
            IOException e
    ) {
        return ResponseEntity.status(
                HttpStatus.BAD_GATEWAY
        ).body(
                Map.of(
                        "ok", false,
                        "error", "Error al comunicarse con Atenea.",
                        "detail", e.getMessage()
                )
        );
    }
}
