
package estrella.tienda.controller;

import estrella.tienda.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatBotController {

    private final ChatService chatService;

    public ChatBotController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<?> chat(
            @RequestBody Map<String, Object> body
    ) throws IOException {

        String question = getQuestion(body);
        int topK = getTopK(body);

        Object answer = chatService.ask(question, topK);

        return ResponseEntity.ok(
                Map.of(
                        "ok", true,
                        "answer", answer
                )
        );
    }


    @PostMapping("/detailed")
    public ResponseEntity<?> detailedChat(
            @RequestBody Map<String, Object> body
    ) throws IOException {

        String question = getQuestion(body);
        int topK = getTopK(body);

        boolean showResources = getBoolean(
                body, "showResources", false
        );

        boolean skipIntent = getBoolean(
                body, "skipIntent", false
        );

        Object answer = chatService.askDetailed(
                question,
                topK,
                skipIntent,
                showResources
        );

        return ResponseEntity.ok(answer);
    }

    @PostMapping("/no-intent")
    public ResponseEntity<?> chatWithoutIntent(
            @RequestBody Map<String, Object> body
    ) throws IOException {

        String question = getQuestion(body);
        int topK = getTopK(body);

        Object answer = chatService.askWithoutIntent(
                question,
                topK
        );

        return ResponseEntity.ok(
                Map.of(
                        "ok", true,
                        "answer", answer
                )
        );
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeIntent(
            @RequestBody Map<String, Object> body
    ) throws IOException {

        String question = getQuestion(body);

        boolean showResources = getBoolean(
                body, "showResources", false
        );

        Object result = chatService.analyzeIntent(
                question,
                showResources
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/reindex")
    public ResponseEntity<?> reindex() throws IOException {

        Object result = chatService.reindex();

        return ResponseEntity.ok(
                Map.of(
                        "ok", true,
                        "answer", result
                )
        );
    }

    private String getQuestion(Map<String, Object> body) {

        Object value = body.get("question");

        if (!(value instanceof String question)
                || question.isBlank()) {

            throw new IllegalArgumentException(
                    "El campo 'question' es obligatorio."
            );
        }

        return question.trim();
    }

    private int getTopK(Map<String, Object> body) {

        Object value = body.get("topK");

        if (value == null) {
            return 8;
        }

        int topK;

        try {
            topK = Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "topK debe ser un número entero."
            );
        }

        if (topK < 1) {
            throw new IllegalArgumentException(
                    "topK debe ser mayor que cero."
            );
        }

        return topK;
    }

    private boolean getBoolean(
            Map<String, Object> body,
            String key,
            boolean defaultValue
    ) {

        Object value = body.get(key);

        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        throw new IllegalArgumentException(
                key + " debe ser true o false."
        );
    }
}
