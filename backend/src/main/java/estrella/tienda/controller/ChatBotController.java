package estrella.tienda.controller;

import estrella.tienda.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
@RestController
public class ChatBotController {
    @Autowired
    ChatService chatService;

    @GetMapping("/chat")
    public String chat(
            @RequestParam String question) {
        try {
            System.out.println(question);
            return chatService.ask(question);
        } catch (IOException e) {
            System.out.println(e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se han podido obtener los productos"
            );
        }
    }
}
