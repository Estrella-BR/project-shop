package estrella.tienda.service;

import estrella.tienda.model.CartItem;
import estrella.tienda.model.Product;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chatbot por reglas: busca productos y arma el carrito de una sesión.
 * Flujo: "añadir leche" -> lista numerada -> "2" (o "2 x3") añade la opción 2.
 */
@Service
public class ChatbotService {

    private static final int OPTIONS = 5;
    private static final int MAX_PENDING_SESSIONS = 2000;

    private static final Pattern ADD_CMD =
            Pattern.compile("^(?:anadir|agregar|agrega|anade|quiero|pon|ponme|busca|buscar|necesito|comprar)\\s+(.+)$");
    private static final Pattern PICK =
            Pattern.compile("^(?:opcion\\s+)?(\\d{1,2})(?:\\s*(?:x|por|unidades|ud|uds)?\\s*(\\d{1,2}))?$");
    private static final Pattern REMOVE =
            Pattern.compile("^(?:quitar|quita|eliminar|elimina|borrar|borra)\\s+(?:el\\s+|la\\s+|linea\\s+|item\\s+)?(\\d{1,3})$");
    private static final Pattern CHANGE =
            Pattern.compile("^(?:cambiar|cambia|modificar)\\s+(?:el\\s+|la\\s+|linea\\s+)?(\\d{1,3})\\s+(?:a|por)\\s+(\\d{1,2})$");
    private static final Pattern LEADING_QTY = Pattern.compile("^(\\d{1,2})\\s*(?:x|unidades|uds?)?\\s+(.+)$");

    public record ChatResult(String reply, List<Product> options) {}

    private final ProductService productService;
    private final CartService cartService;

    private final Map<String, List<Product>> pending = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<Product>> e) {
                    return size() > MAX_PENDING_SESSIONS;
                }
            });
    private final Map<String, Integer> pendingQty = new java.util.concurrent.ConcurrentHashMap<>();

    public ChatbotService(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }

    public ChatResult handle(String sessionId, String rawMessage) {
        String msg = normalize(rawMessage);

        if (msg.isEmpty()) {
            return new ChatResult("Escribe algo como \"añadir leche\" o \"ayuda\".", List.of());
        }

        Matcher m;

        if (msg.matches("^(ayuda|help|\\?|hola|buenas|menu)$")) {
            return new ChatResult(help(), List.of());
        }
        if (msg.matches("^(carrito|ver carrito|mi carrito|ver|lista)$")) {
            return new ChatResult(describeCart(sessionId), List.of());
        }
        if (msg.matches("^(total|cuanto es|cuanto llevo)$")) {
            List<CartItem> cart = cartService.getCart(sessionId);
            return new ChatResult(cart.isEmpty() ? "Tu carrito está vacío."
                    : "Total del carrito: " + money(CartService.total(cart)), List.of());
        }
        if (msg.matches("^(vaciar|vaciar carrito|quitar todo|borrar todo|limpiar)$")) {
            cartService.clear(sessionId);
            pending.remove(sessionId);
            return new ChatResult("He vaciado tu carrito.", List.of());
        }
        if ((m = REMOVE.matcher(msg)).matches()) {
            CartItem removed = cartService.remove(sessionId, Integer.parseInt(m.group(1)));
            return new ChatResult(removed == null
                    ? "No existe esa línea en tu carrito. Escribe \"carrito\" para verlas."
                    : "Quitado: " + removed.getName() + ".", List.of());
        }
        if ((m = CHANGE.matcher(msg)).matches()) {
            CartItem it = cartService.setUnits(sessionId,
                    Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            return new ChatResult(it == null
                    ? "No existe esa línea en tu carrito."
                    : "Ahora llevas " + it.getUnits() + " x " + it.getName() + ".", List.of());
        }

        List<Product> options = pending.get(sessionId);
        if (options != null && (m = PICK.matcher(msg)).matches()) {
            int index = Integer.parseInt(m.group(1));
            int units = m.group(2) != null ? Integer.parseInt(m.group(2))
                    : pendingQty.getOrDefault(sessionId, 1);
            if (index < 1 || index > options.size()) {
                return new ChatResult("Elige un número entre 1 y " + options.size() + ".", options);
            }
            if (units < 1) {
                return new ChatResult("La cantidad debe ser al menos 1.", options);
            }
            CartItem item = cartService.add(sessionId, options.get(index - 1), units);
            if (item == null) {
                return new ChatResult("Tu carrito está lleno (máximo " + CartService.MAX_LINES + " productos).", List.of());
            }
            pending.remove(sessionId);
            pendingQty.remove(sessionId);
            return new ChatResult("Añadido: " + units + " x " + item.getName()
                    + " (" + item.getSeller() + "). Ahora llevas " + item.getUnits()
                    + " de este producto. Total: " + money(CartService.total(cartService.getCart(sessionId)))
                    + ".\n¿Algo más? Escribe \"añadir ...\" o \"carrito\".", List.of());
        }

        if ((m = ADD_CMD.matcher(msg)).matches()) {
            return search(sessionId, m.group(1));
        }

        // Sin comando: lo tratamos como una búsqueda directa.
        return search(sessionId, msg);
    }

    private ChatResult search(String sessionId, String query) {
        int units = 1;
        Matcher q = LEADING_QTY.matcher(query);
        if (q.matches()) {
            units = Integer.parseInt(q.group(1));
            query = q.group(2);
        }

        String sort = null;
        if (query.matches(".*\\b(barato|barata|economico|economica)\\b.*")) {
            sort = "asc";
            query = query.replaceAll("\\b(el mas |la mas |mas )?(barato|barata|economico|economica)\\b", "").trim();
        }
        query = query.replaceAll("\\b(de|del|el|la|los|las|un|una|unos|unas)\\b", " ").replaceAll("\\s+", " ").trim();

        if (query.isEmpty()) {
            return new ChatResult("¿Qué producto quieres buscar?", List.of());
        }

        List<Product> found;
        try {
            found = searchCatalog(query, sort);
        } catch (IOException | RuntimeException e) {
            return new ChatResult("No he podido consultar el catálogo ahora mismo. Inténtalo de nuevo en un momento.", List.of());
        }

        if (found.isEmpty()) {
            pending.remove(sessionId);
            return new ChatResult("No he encontrado \"" + query + "\". Prueba con otra palabra.", List.of());
        }

        pending.put(sessionId, found);
        pendingQty.put(sessionId, units);

        StringBuilder sb = new StringBuilder("He encontrado esto para \"" + query + "\":\n");
        for (int i = 0; i < found.size(); i++) {
            Product p = found.get(i);
            sb.append(i + 1).append(". ").append(p.getName());
            if (p.getQuantity() != null && !p.getQuantity().isBlank()) sb.append(" (").append(p.getQuantity()).append(")");
            sb.append(" - ").append(money(CartService.parsePrice(p.getPrice())))
              .append(" - ").append(p.getSeller()).append("\n");
        }
        sb.append("Responde con el número").append(units > 1 ? " (se añadirán " + units + " unidades)" : "")
          .append(", por ejemplo \"1\" o \"2 x3\".");
        return new ChatResult(sb.toString(), found);
    }

    private List<Product> searchCatalog(String query, String sort) throws IOException {
        List<Product> all = productService.searchByName(query);
        if (all.isEmpty() && query.contains(" ")) {
            // Reintento con la primera palabra si la frase completa no coincide.
            all = productService.searchByName(query.split(" ")[0]);
        }
        String key = query.split(" ")[0];
        // Relevancia: el nombre empieza por la búsqueda > la contiene como palabra > subcadena.
        Comparator<Product> byRelevance = Comparator.comparingInt(p -> relevance(p, key));
        if ("asc".equals(sort)) {
            byRelevance = byRelevance.thenComparingDouble(p -> CartService.parsePrice(p.getPrice()));
        }
        all.sort(byRelevance);
        return new ArrayList<>(all.subList(0, Math.min(OPTIONS, all.size())));
    }

    private static int relevance(Product p, String key) {
        String name = normalize(p.getName());
        if (name.startsWith(key)) return 0;
        if (name.matches(".*\\b" + Pattern.quote(key) + ".*")) return 1;
        return 2;
    }

    private String describeCart(String sessionId) {
        List<CartItem> cart = cartService.getCart(sessionId);
        if (cart.isEmpty()) {
            return "Tu carrito está vacío. Prueba con \"añadir leche\".";
        }
        StringBuilder sb = new StringBuilder("Tu carrito:\n");
        for (int i = 0; i < cart.size(); i++) {
            CartItem it = cart.get(i);
            sb.append(i + 1).append(". ").append(it.getUnits()).append(" x ").append(it.getName());
            if (it.getQuantityLabel() != null && !it.getQuantityLabel().isBlank()) sb.append(" (").append(it.getQuantityLabel()).append(")");
            sb.append(" - ").append(money(it.getUnitPrice() * it.getUnits()))
              .append(" - ").append(it.getSeller()).append("\n");
        }
        sb.append("Total: ").append(money(CartService.total(cart)));
        return sb.toString();
    }

    private static String help() {
        return """
                Puedo armar tu carrito. Prueba con:
                - "añadir leche" o "añadir 3 yogur" (busca y te da opciones)
                - "añadir aceite barato" (ordena por precio)
                - "1" o "2 x3" para elegir una opción
                - "carrito" y "total"
                - "quitar 2" (línea del carrito), "cambiar 1 a 4"
                - "vaciar" para borrar todo""";
    }

    private static String money(double v) {
        return String.format(Locale.US, "%.2f €", v);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[¿?¡!.,;:]+$", "")
                .replaceAll("\\s+", " ");
        return n.length() > 200 ? n.substring(0, 200) : n;
    }
}
