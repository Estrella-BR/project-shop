
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ChatService {

    private static final Logger log =
            LoggerFactory.getLogger(ChatService.class);

    private final Path pythonScript;
    private final String pythonCommand;
    private final long timeoutSeconds;

    private final ObjectMapper mapper = new ObjectMapper();

    public ChatService(
            @Value("${app.python.command:python}") String command,
            @Value("${app.python.chat-timeout-seconds:120}") long timeoutSeconds
    ) {

        this.pythonScript = Path.of(
                "C:\\Users\\Estrella\\Codigo\\Shop\\Ia\\chat.py"
        ).toAbsolutePath().normalize();

        this.pythonCommand = command;
        this.timeoutSeconds = timeoutSeconds;
    }

    @PostConstruct
    void init() throws IOException {

        if (!Files.exists(pythonScript)) {
            throw new IllegalStateException(
                    "No existe el script de chat: " + pythonScript
            );
        }

        log.info(
                "ChatService listo -> script={}, python={}, timeout={}s",
                pythonScript,
                pythonCommand,
                timeoutSeconds
        );
    }

    public String ask(String question) throws IOException {
        return ask(question, 5);
    }

    public String ask(String question, int topK) throws IOException {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "La pregunta no puede estar vacía."
            );
        }

        if (topK < 1) {
            throw new IllegalArgumentException(
                    "topK debe ser mayor que cero."
            );
        }

        ProcessBuilder pb = new ProcessBuilder(
                pythonCommand,
                pythonScript.toString(),
                "--question", question,
                "--top-k", String.valueOf(topK)
        );

        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Leer la salida mientras Python está ejecutándose
        CompletableFuture<String> outputFuture =
                CompletableFuture.supplyAsync(() -> {

                    try (var input = process.getInputStream()) {

                        return new String(
                                input.readAllBytes(),
                                StandardCharsets.UTF_8
                        );

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        try {

            boolean finished = process.waitFor(
                    timeoutSeconds,
                    TimeUnit.SECONDS
            );

            if (!finished) {

                process.destroyForcibly();

                throw new IOException(
                        "Atenea ha superado el tiempo máximo de ejecución."
                );
            }

            String output = outputFuture.get(
                    10,
                    TimeUnit.SECONDS
            ).trim();

            if (output.isEmpty()) {
                throw new IOException(
                        "Python no ha devuelto ninguna respuesta."
                );
            }

            log.debug("Respuesta completa de Python: {}", output);

            // Extraer exclusivamente el JSON
            String json = extractJson(output);

            JsonNode node;

            try {
                node = mapper.readTree(json);

            } catch (Exception e) {

                throw new IOException(
                        "No se pudo interpretar el JSON de Atenea: " + json,
                        e
                );
            }

            if (!node.path("ok").asBoolean(false)) {

                throw new IOException(
                        "Error de Atenea: "
                                + node.path("error").asText(
                                "Error desconocido"
                        )
                );
            }

            String answer = node.path("answer").asText();

            if (answer.isBlank()) {
                throw new IOException(
                        "Atenea ha devuelto una respuesta vacía."
                );
            }

            return answer;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            process.destroyForcibly();

            throw new IOException(
                    "La ejecución de Atenea fue interrumpida.",
                    e
            );

        } catch (java.util.concurrent.ExecutionException
                 | java.util.concurrent.TimeoutException e) {

            process.destroyForcibly();

            throw new IOException(
                    "No se pudo obtener la respuesta de Python.",
                    e
            );
        }
    }

    /**
     * Busca el JSON de respuesta al final de la salida de Python.
     * Permite que existan mensajes informativos antes del JSON.
     */
    private String extractJson(String output) throws IOException {

        String[] lines = output.split("\\R");

        for (int i = lines.length - 1; i >= 0; i--) {

            String line = lines[i].trim();

            if (line.startsWith("{") && line.endsWith("}")) {
                return line;
            }
        }

        throw new IOException(
                "No se encontró el JSON de respuesta de Python: "
                        + output
        );
    }
}
