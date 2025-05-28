package hr.fer.dipl.db.repository;

import hr.fer.dipl.db.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    CartItem findByUserIdAndProductId(Long userId, Long productId);
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);
}