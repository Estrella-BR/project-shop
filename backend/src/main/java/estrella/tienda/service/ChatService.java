
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
            @Value("${app.python.script:../Ia/chat.py}") String scriptPath,
            @Value("${app.python.chat-timeout-seconds:180}") long timeoutSeconds
    ) {
        this.pythonScript = Path.of(scriptPath)
                .toAbsolutePath()
                .normalize();

        this.pythonCommand = command;
        this.timeoutSeconds = timeoutSeconds;
    }

    // ======================================================
    // INICIALIZACION
    // ======================================================

    @PostConstruct
    void init() {

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

    // ======================================================
    // CONSULTAS
    // ======================================================

    public Object ask(String question) throws IOException {
        return ask(question, 8);
    }

    public Object ask(String question, int topK) throws IOException {

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

        log.info("Nueva consulta a Atenea: {}", question);

        // --------------------------------------------------
        // EJECUTAR PYTHON
        // --------------------------------------------------

        ProcessBuilder pb = new ProcessBuilder(
                pythonCommand,
                pythonScript.toString(),
                "--question",
                question,
                "--top-k",
                String.valueOf(topK)
        );

        // Mantener stdout y stderr separados.
        // stdout contiene el JSON y stderr los mensajes de Python.
        pb.redirectErrorStream(false);

        Process process = pb.start();

        // Leer ambos streams en paralelo para evitar bloqueos.
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

        CompletableFuture<String> errorFuture =
                CompletableFuture.supplyAsync(() -> {
                    try (var input = process.getErrorStream()) {
                        return new String(
                                input.readAllBytes(),
                                StandardCharsets.UTF_8
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        try {

            // --------------------------------------------------
            // ESPERAR A PYTHON
            // --------------------------------------------------

            boolean finished = process.waitFor(
                    timeoutSeconds,
                    TimeUnit.SECONDS
            );

            if (!finished) {

                process.destroyForcibly();

                throw new IOException(
                        "Atenea ha superado el tiempo máximo de ejecución ("
                                + timeoutSeconds + "s)."
                );
            }

            String output = outputFuture.get(
                    10,
                    TimeUnit.SECONDS
            ).trim();

            String errorOutput = errorFuture.get(
                    10,
                    TimeUnit.SECONDS
            ).trim();

            if (!errorOutput.isEmpty()) {
                log.warn("Mensajes de Python:\n{}", errorOutput);
            }

            if (output.isEmpty()) {
                throw new IOException(
                        "Python no ha devuelto ninguna respuesta."
                );
            }

            // --------------------------------------------------
            // COMPROBAR CODIGO DE SALIDA
            // --------------------------------------------------

            int exitCode = process.exitValue();

            if (exitCode != 0) {

                log.error("Error ejecutando Python: {}", output);

                throw new IOException(
                        "Error ejecutando Atenea: " + output
                );
            }

            // --------------------------------------------------
            // PARSEAR JSON
            // --------------------------------------------------

            JsonNode node = mapper.readTree(output);

            if (node == null || !node.isObject()) {
                throw new IOException(
                        "Python no ha devuelto un JSON válido."
                );
            }

            if (!node.path("ok").asBoolean(false)) {

                throw new IOException(
                        "Error de Atenea: "
                                + node.path("error")
                                .asText("Error desconocido")
                );
            }

            JsonNode answer = node.path("answer");

            if (answer.isMissingNode() || answer.isNull()) {
                throw new IOException(
                        "Atenea no ha devuelto ninguna respuesta."
                );
            }

            log.info("Respuesta de Atenea recibida correctamente.");

            // Devolvemos directamente el contenido de answer.
            // Puede ser un objeto JSON o un texto de rechazo.
            return mapper.convertValue(answer, Object.class);
        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            process.destroyForcibly();

            throw new IOException(
                    "La ejecución de Atenea fue interrumpida.",
                    e
            );

        } catch (ExecutionException | TimeoutException e) {

            process.destroyForcibly();

            throw new IOException(
                    "No se pudo obtener la respuesta de Python.",
                    e
            );

        } finally {

            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
