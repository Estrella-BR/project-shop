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

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final Path pythonScript;
    private final String pythonCommand;
    private final long timeoutSeconds;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatService(
            @Value("${app.python.command:python3}") String command,
            @Value("${app.python.chat-timeout-seconds:120}") long timeoutSeconds
    ) {
        this.pythonScript   = Path.of("C:\\Users\\Estrella\\Codigo\\Shop\\Ia\\chat.py").toAbsolutePath().normalize();
        this.pythonCommand  =
                System.getenv().getOrDefault("PYTHON_COMMAND", "python");;
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
        return ask(question, 3);
    }

    public String ask(String question, int topK) throws IOException {

        ProcessBuilder pb = new ProcessBuilder(
                pythonCommand,
                pythonScript.toString(),
                "--question", question,
                "--top-k", String.valueOf(topK)
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Leer stdout en paralelo para que el timeout funcione de verdad
        CompletableFuture<String> outFuture = CompletableFuture.supplyAsync(() -> {
            try (var is = process.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        });

        String output;
        try {
            output = outFuture.get(50, TimeUnit.SECONDS).trim();
        } catch (Exception e) {
            throw new IOException("No se pudo leer la salida de Python", e);
        }

        if (output.isEmpty()) {
            throw new IOException("Python no devolvió nada (exit=" + process.exitValue() + ")");
        }

        JsonNode node;
        try {
            node = mapper.readTree(output);
        } catch (Exception e) {
            throw new IOException("Respuesta inválida de Python: " + output, e);
        }

        if (!node.path("ok").asBoolean(false)) {
            throw new IOException("Python reportó error: " + node.path("error").asText());
        }

        return node.path("answer").asText();
    }
}