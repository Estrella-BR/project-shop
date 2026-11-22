package estrella.tienda.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final Path pythonScript;
    private final String pythonCommand;
    private final long timeoutSeconds;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatService(
            @Value("${app.python.command:python}") String command,
            @Value("${app.python.chat-timeout-seconds:120}") long timeoutSeconds
    ) {
        this.pythonScript = Path.of("..", "Ia", "chat.py")
                .toAbsolutePath()
                .normalize();
        this.pythonCommand = command;
        this.timeoutSeconds = timeoutSeconds;
    }

    @PostConstruct
    void init() throws IOException {
        if (!Files.exists(pythonScript)) {
            throw new IllegalStateException("No existe el script de chat: " + pythonScript);
        }
        log.info("ChatService listo -> script={}, python={}, timeout={}s",
                pythonScript, pythonCommand, timeoutSeconds);
    }

    public String ask(String question) throws IOException {
        return ask(question, 5);
    }

    public String ask(String question, int topK) throws IOException {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía.");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("topK debe ser mayor que cero.");
        }

        ProcessBuilder pb = new ProcessBuilder(
                pythonCommand,
                pythonScript.toString(),
                "--question", question,
                "--top-k", String.valueOf(topK)
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Leer la salida en paralelo para evitar deadlocks si el pipe se llena.
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
            try (var input = process.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException(
                        "Atenea ha superado el tiempo máximo de ejecución ("
                                + timeoutSeconds + "s).");
            }

            String output = outputFuture.get(10, TimeUnit.SECONDS).trim();
            if (output.isEmpty()) {
                throw new IOException("Python no ha devuelto ninguna respuesta.");
            }

            log.debug("Salida completa de Python:\n{}", output);

            JsonNode node = extractJsonNode(output);

            if (!node.path("ok").asBoolean(false)) {
                throw new IOException("Error de Atenea: "
                        + node.path("error").asText("Error desconocido"));
            }

            String answer = node.path("answer").asText();
            if (answer.isBlank()) {
                throw new IOException("Atenea ha devuelto una respuesta vacía.");
            }
            return answer;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("La ejecución de Atenea fue interrumpida.", e);
        } catch (ExecutionException | TimeoutException e) {
            process.destroyForcibly();
            throw new IOException("No se pudo obtener la respuesta de Python.", e);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Extrae el JSON de la respuesta.
     *
     * Python usa json.dumps(..., indent=4), por lo que el JSON ocupa varias
     * líneas y la comprobación "line starts with { and ends with }" no funciona.
     *
     * Estrategia:
     *  1. Recoger todas las líneas cuyo contenido (trim) sea exactamente "{".
     *     Los objetos anidados dentro de arrays también cumplen esta condición,
     *     pero al intentar parsear desde ellos fallará (tienen llaves de cierre
     *     extra al final).
     *  2. Probar desde el candidato más reciente hacia atrás y devolver el
     *     primer fragmento que parsee como JSON válido y que contenga "ok".
     *  3. Fallback: JSON compacto en una sola línea (por si en el futuro se
     *     cambia el script a indent=None).
     */
    private JsonNode extractJsonNode(String output) throws IOException {

        String[] lines = output.split("\\R");

        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals("{")) {
                starts.add(i);
            }
        }

        // Intentar desde el inicio más reciente hacia atrás
        for (int idx = starts.size() - 1; idx >= 0; idx--) {
            int start = starts.get(idx);
            String candidate = String.join("\n",
                    Arrays.copyOfRange(lines, start, lines.length));
            try {
                JsonNode node = mapper.readTree(candidate);
                if (node != null && node.isObject() && node.has("ok")) {
                    return node;
                }
            } catch (Exception ignored) {
                // siguiente candidato
            }
        }

        // Fallback: JSON compacto en una línea
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                try {
                    JsonNode node = mapper.readTree(line);
                    if (node != null && node.isObject() && node.has("ok")) {
                        return node;
                    }
                } catch (Exception ignored) {
                    // siguiente
                }
            }
        }

        throw new IOException(
                "No se encontró un JSON de respuesta válido en la salida de Python:\n"
                        + output);
    }
}