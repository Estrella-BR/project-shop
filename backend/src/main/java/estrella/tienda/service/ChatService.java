
package estrella.tienda.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            @Value("${app.python.script:../Ia/main.py}") String scriptPath,
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
        return ask(question, topK, false, false);
    }

    public Object ask(String question) throws IOException {
        return ask(question, 8);
    }

    public Object ask(
            String question,
            int topK,
            boolean skipIntent,
            boolean showResources
    ) throws IOException {

        JsonNode result = executeQuestion(
                question,
                topK,
                skipIntent,
                showResources
        );

        JsonNode answer = result.path("answer");

        if (answer.isMissingNode() || answer.isNull()) {
            throw new IOException(
                    "Atenea no ha devuelto ninguna respuesta."
            );
        }

        return mapper.convertValue(answer, Object.class);
    }

    public Object askWithoutIntent(
            String question,
            int topK
    ) throws IOException {

        return ask(question, topK, true, false);
    }

    public Object askWithoutIntent(
            String question
    ) throws IOException {

        return askWithoutIntent(question, 8);
    }

    public Object askDetailed(
            String question,
            int topK,
            boolean skipIntent,
            boolean showResources
    ) throws IOException {

        JsonNode result = executeQuestion(
                question,
                topK,
                skipIntent,
                showResources
        );

        return mapper.convertValue(result, Object.class);
    }

    public Object askDetailed(String question) throws IOException {
        return askDetailed(question, 8, false, false);
    }

    public Object analyzeIntent(
            String question,
            boolean showResources
    ) throws IOException {

        validateQuestion(question);

        List<String> args = new ArrayList<>();

        args.add("--question");
        args.add(question);

        args.add("--analyze-only");

        if (showResources) {
            args.add("--show-resources");
        }

        JsonNode result = executePython(
                args,
                true,
                timeoutSeconds
        );

        return mapper.convertValue(result, Object.class);
    }

    public Object analyzeIntent(String question) throws IOException {
        return analyzeIntent(question, false);
    }

    public Object reindex() throws IOException {

        log.info("Iniciando reconstrucción del índice de Atenea...");

        JsonNode result = executePython(
                List.of("--reindex"),
                false,
                1800
        );

        return mapper.convertValue(result, Object.class);
    }

    private JsonNode executeQuestion(
            String question,
            int topK,
            boolean skipIntent,
            boolean showResources
    ) throws IOException {

        validateQuestion(question);

        if (topK < 1) {
            throw new IllegalArgumentException(
                    "topK debe ser mayor que cero."
            );
        }

        List<String> args = new ArrayList<>();

        args.add("--question");
        args.add(question);

        args.add("--top-k");
        args.add(String.valueOf(topK));

        if (skipIntent) {
            args.add("--no-analyze");
        }

        if (showResources) {
            args.add("--show-resources");
        }

        log.info(
                "Nueva consulta a Atenea: skipIntent={}, topK={}",
                skipIntent,
                topK
        );

        return executePython(args, true, timeoutSeconds);
    }

    private void validateQuestion(String question) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "La pregunta no puede estar vacía."
            );
        }
    }

    private JsonNode executePython(
            List<String> argumentos,
            boolean requireJson,
            long timeoutSegundos
    ) throws IOException {

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
                CompletableFuture.supplyAsync(
                        () -> readStream(process.getInputStream())
                );

        CompletableFuture<String> stderrFuture =
                CompletableFuture.supplyAsync(
                        () -> readStream(process.getErrorStream())
                );

        try {

            boolean finished = process.waitFor(
                    timeoutSegundos,
                    TimeUnit.SECONDS
            );

            if (!finished) {
                process.destroyForcibly();

                throw new IOException(
                        "Atenea ha superado el tiempo máximo de ejecución ("
                                + timeoutSegundos + "s)."
                );
            }

            String stdout = stdoutFuture.get(
                    10, TimeUnit.SECONDS
            ).trim();

            String stderr = stderrFuture.get(
                    10, TimeUnit.SECONDS
            ).trim();

            if (!stderr.isEmpty()) {
                log.warn("stderr de Python:\n{}", stderr);
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {

                log.error(
                        "Python falló. exit={} stdout='{}' stderr='{}'",
                        exitCode,
                        stdout,
                        stderr
                );

                throw new IOException(
                        "Python falló (exit " + exitCode + "). "
                                + "stdout='" + stdout
                                + "' stderr='" + stderr + "'"
                );
            }

            // Operaciones sin JSON
            if (!requireJson) {

                var result = mapper.createObjectNode();

                result.put("ok", true);
                result.put("raw", stdout);

                return result;
            }

            if (stdout.isEmpty()) {
                throw new IOException(
                        "Python no escribió nada por stdout. stderr: "
                                + stderr
                );
            }

            JsonNode node;

            try {
                node = mapper.readTree(stdout);

            } catch (Exception e) {

                log.error(
                        "JSON inválido. stdout='{}' stderr='{}'",
                        stdout,
                        stderr
                );

                throw new IOException(
                        "Python no devolvió JSON válido:\n"
                                + stdout + "\nstderr:\n" + stderr,
                        e
                );
            }

            if (node == null || !node.isObject()) {
                throw new IOException(
                        "Python no devolvió un objeto JSON."
                );
            }

            if (!node.path("ok").asBoolean(false)) {
                throw new IOException(
                        "Error de Atenea: "
                                + node.path("error")
                                .asText("Error desconocido")
                );
            }

            return node;

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

    private static String readStream(InputStream input) {

        try (input) {
            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
