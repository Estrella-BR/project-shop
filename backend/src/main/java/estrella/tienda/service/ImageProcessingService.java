package estrella.tienda.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ImageProcessingService {

    private static final Path IMAGE_DIRECTORY =
            Path.of("data", "images").toAbsolutePath().normalize();

    private static final Path PYTHON_SCRIPT =
            Path.of("remove_background.py").toAbsolutePath().normalize();

    private static final String PYTHON_COMMAND =
            System.getenv().getOrDefault("PYTHON_COMMAND", "python");

    public ImageProcessingService() {
       /* try {
            Files.createDirectories(IMAGE_DIRECTORY);

            if (!Files.exists(PYTHON_SCRIPT)) {
                throw new IOException(
                        "No se encuentra remove_background.py: " + PYTHON_SCRIPT
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Error inicializando el procesamiento de imágenes", e
            );
        }*/
    }

    public String processImage(String imageUrl) {
        return processImage(imageUrl, null);
    }

    public String processImage(String imageUrl, String customName) {

        if (imageUrl == null || imageUrl.isBlank()) {
            return imageUrl;
        }

        String id;
        if (customName != null && !customName.isBlank()) {

            id = customName.trim().replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        } else {
            id = UUID.randomUUID().toString();
        }

        Path original = null;
        Path processed = IMAGE_DIRECTORY.resolve(id + ".png");

        try {
            Connection.Response response = Jsoup.connect(imageUrl)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .userAgent("Mozilla/5.0")
                    .timeout(20000)
                    .maxBodySize(10 * 1024 * 1024)
                    .execute();

            if (response.statusCode() != 200) {
                System.err.println("Error descargando imagen: " + imageUrl);
                return imageUrl;
            }

            String contentType = response.contentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                System.err.println("La URL no devuelve una imagen: " + imageUrl);
                return imageUrl;
            }

            byte[] imageBytes = response.bodyAsBytes();
            if (imageBytes.length == 0) {
                return imageUrl;
            }

            original = Files.createTempFile(IMAGE_DIRECTORY, "original_", ".tmp");
            Files.write(original, imageBytes);

            ProcessBuilder builder = new ProcessBuilder(
                    PYTHON_COMMAND,
                    PYTHON_SCRIPT.toString(),
                    original.toAbsolutePath().toString(),
                    processed.toAbsolutePath().toString(),
                    "--public-id", id
            );

            builder.redirectErrorStream(true);
            Process process = builder.start();

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Tiempo de procesamiento agotado");
            }

            if (process.exitValue() != 0 || !Files.exists(processed) || Files.size(processed) == 0) {
                throw new IOException("Error de Python: " + output);
            }

            return "/images/" + id + ".png";

        } catch (Exception e) {
            System.err.println("No se pudo procesar la imagen " + imageUrl + ": " + e.getMessage());
            try {
                Files.deleteIfExists(processed);
            } catch (IOException ignored) {}
            return imageUrl;
        } finally {
            if (original != null) {
                try {
                    Files.deleteIfExists(original);
                } catch (IOException ignored) {}
            }
        }
    }
}