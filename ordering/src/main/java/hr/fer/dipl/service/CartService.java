package hr.fer.dipl.service;

import hr.fer.dipl.db.model.CartItem;
import hr.fer.dipl.dto.CartItemDTO;

import java.util.List;

public interface CartService {

    List<CartItemDTO> getCartItems();

    CartItemDTO addToCart(CartItemDTO cartItemDTO);

    void removeFromCart(Long cartItemId);

    CartItemDTO updateCartItem(Long cartItemId, CartItemDTO cartItemDTO);

    void clearCart();

    List<CartItem> getCartItemsForUser(Long userId);

    void clearCartForUser(Long userId);
}
