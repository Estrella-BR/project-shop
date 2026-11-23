package estrella.tienda.controller;

import com.fasterxml.jackson.databind.JsonNode;
import estrella.tienda.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatBotController {
    @Autowired
    ChatService chatService;


    @PostMapping
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body)
            throws IOException {

        String question = body.get("question");

        Object answer = chatService.ask(question);

        return ResponseEntity.ok(
                Map.of(
                        "ok", true,
                        "answer", answer
                )
        );
    }
}
