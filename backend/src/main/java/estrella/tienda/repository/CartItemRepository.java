package estrella.tienda.repository;

import estrella.tienda.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findBySessionIdOrderByIdAsc(String sessionId);

    Optional<CartItem> findBySessionIdAndProductId(String sessionId, String productId);

    long countBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
