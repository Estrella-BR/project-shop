package estrella.tienda.service;

import estrella.tienda.model.CartItem;
import estrella.tienda.model.Product;
import estrella.tienda.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CartService {

    public static final int MAX_LINES = 100;
    public static final int MAX_UNITS = 99;

    private final CartItemRepository repository;

    public CartService(CartItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCart(String sessionId) {
        return repository.findBySessionIdOrderByIdAsc(sessionId);
    }

    /** Añade unidades de un producto. Devuelve la línea resultante, o null si el carrito está lleno. */
    public CartItem add(String sessionId, Product product, int units) {
        String productId = productKey(product);

        CartItem item = repository.findBySessionIdAndProductId(sessionId, productId).orElse(null);

        if (item == null) {
            if (repository.countBySessionId(sessionId) >= MAX_LINES) {
                return null;
            }
            item = new CartItem();
            item.setSessionId(sessionId);
            item.setProductId(productId);
            item.setName(truncate(product.getName(), 300));
            item.setQuantityLabel(truncate(product.getQuantity(), 100));
            item.setImage(truncate(product.getImage(), 1000));
            item.setSeller(truncate(product.getSeller(), 50));
            item.setUnitPrice(parsePrice(product.getPrice()));
            item.setUnits(0);
        }

        item.setUnits(Math.min(MAX_UNITS, item.getUnits() + units));
        return repository.save(item);
    }

    /** Fija las unidades de la línea n (1-based). Devuelve la línea o null si no existe. */
    public CartItem setUnits(String sessionId, int line, int units) {
        List<CartItem> cart = getCart(sessionId);
        if (line < 1 || line > cart.size()) {
            return null;
        }
        CartItem item = cart.get(line - 1);
        item.setUnits(Math.max(1, Math.min(MAX_UNITS, units)));
        return repository.save(item);
    }

    /** Quita la línea n (1-based). Devuelve la línea quitada o null si no existe. */
    public CartItem remove(String sessionId, int line) {
        List<CartItem> cart = getCart(sessionId);
        if (line < 1 || line > cart.size()) {
            return null;
        }
        CartItem item = cart.get(line - 1);
        repository.delete(item);
        return item;
    }

    public void clear(String sessionId) {
        repository.deleteBySessionId(sessionId);
    }

    public static double total(List<CartItem> cart) {
        return cart.stream().mapToDouble(i -> i.getUnitPrice() * i.getUnits()).sum();
    }

    private static String productKey(Product p) {
        if (p.getId() != null && !p.getId().isBlank()) {
            return truncate(p.getId(), 200);
        }
        return truncate(p.getSeller() + "|" + p.getName() + "|" + p.getQuantity(), 200);
    }

    static double parsePrice(String price) {
        if (price == null || price.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(price.replaceAll("[^0-9,.]", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
