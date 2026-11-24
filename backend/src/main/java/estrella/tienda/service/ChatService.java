
package estrella.tienda.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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


    public Object ask(String question, int topK) throws IOException {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía.");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("topK debe ser mayor que cero.");
        }

        log.info("Nueva consulta a Atenea: {}", question);

        JsonNode node = executePython(
                List.of("--question", question, "--top-k", String.valueOf(topK)),
                true,
                timeoutSeconds
        );

        JsonNode answer = node.path("answer");
        if (answer.isMissingNode() || answer.isNull()) {
            throw new IOException("Atenea no ha devuelto ninguna respuesta.");
        }

        return mapper.convertValue(answer, Object.class);
    }

    public Object reindex() throws IOException {

        log.info("Lanzando reindex de Atenea...");

        JsonNode node = executePython(
                List.of("--reindex"),
                false,
                1800
        );

        return mapper.convertValue(node, Object.class);
    }

    public Object ask(String question) throws IOException {
        return ask(question, 8);
    }

    private JsonNode executePython(List<String> argumentos,
                                   boolean requireJson,
                                   long timeoutSegundos) throws IOException {

        List<String> comando = new ArrayList<>();
        comando.add(pythonCommand);
        comando.add("-u");
        comando.add(pythonScript.toString());
        comando.addAll(argumentos);

        log.info("Ejecutando Python: {}", comando);

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        CompletableFuture<String> stdoutFuture =
                CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
        CompletableFuture<String> stderrFuture =
                CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

        try {
            boolean finished = process.waitFor(timeoutSegundos, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException(
                        "Atenea ha superado el tiempo máximo de ejecución ("
                                + timeoutSegundos + "s).");
            }

            String stdout = stdoutFuture.get(10, TimeUnit.SECONDS).trim();
            String stderr = stderrFuture.get(10, TimeUnit.SECONDS).trim();

            if (!stderr.isEmpty()) {
                log.warn("stderr de Python:\n{}", stderr);
            }

            int exitCode = process.exitValue();

            // ---------- 1. Código de salida distinto de cero ----------
            if (exitCode != 0) {
                log.error("Python falló. exit={} stdout='{}' stderr='{}'",
                        exitCode, stdout, stderr);
                throw new IOException(
                        "Python falló (exit " + exitCode + "). "
                                + "stdout='" + stdout + "' stderr='" + stderr + "'");
            }

            // ---------- 2. Modo no-JSON (reindex y similares) ----------
            if (!requireJson) {
                log.info("Python terminó correctamente (sin JSON requerido).");
                ObjectNode ok = mapper.createObjectNode();
                ok.put("ok", true);
                ok.put("raw", stdout);
                return ok;
            }

            // ---------- 3. Modo estricto: esperamos JSON ----------
            if (stdout.isEmpty()) {
                log.error("Python terminó con exit 0 pero stdout vacío. stderr='{}'", stderr);
                throw new IOException(
                        "chat.py no escribió nada por stdout. "
                                + "stderr fue:\n" + stderr);
            }

            JsonNode node;
            try {
                node = mapper.readTree(stdout);
            } catch (Exception e) {
                log.error("JSON inválido. stdout='{}' stderr='{}'", stdout, stderr);
                throw new IOException(
                        "Python no devolvió JSON válido:\n" + stdout
                                + "\nstderr:\n" + stderr, e);
            }

            if (node == null || !node.isObject()) {
                throw new IOException(
                        "Python no devolvió un objeto JSON:\n" + stdout);
            }

            if (!node.path("ok").asBoolean(false)) {
                throw new IOException(
                        "Error de Atenea: "
                                + node.path("error").asText("Error desconocido"));
            }

            return node;

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

    private JsonNode executePython(List<String> argumentos, boolean requireJson)
            throws IOException {
        return executePython(argumentos, requireJson, timeoutSeconds);
    }

    private static String readStream(InputStream input) {
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
