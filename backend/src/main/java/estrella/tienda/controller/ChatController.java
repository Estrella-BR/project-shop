package estrella.tienda.controller;

import estrella.tienda.model.CartItem;
import estrella.tienda.service.CartService;
import estrella.tienda.service.ChatbotService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
public class ChatController {

    public record ChatRequest(String sessionId, String message) {}

    public record CartView(List<CartItem> items, double total) {}

    public record ChatResponse(String reply, CartView cart) {}

    private final ChatbotService chatbot;
    private final CartService cartService;

    public ChatController(ChatbotService chatbot, CartService cartService) {
        this.chatbot = chatbot;
        this.cartService = cartService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String sessionId = validate(request.sessionId());
        ChatbotService.ChatResult result = chatbot.handle(sessionId, request.message());
        return new ChatResponse(result.reply(), view(sessionId));
    }

    @GetMapping("/cart")
    public CartView cart(@RequestParam String sessionId) {
        return view(validate(sessionId));
    }

    private CartView view(String sessionId) {
        List<CartItem> items = cartService.getCart(sessionId);
        return new CartView(items, CartService.total(items));
    }

    private static String validate(String sessionId) {
        try {
            return UUID.fromString(sessionId).toString();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId inválido");
        }
    }
}
