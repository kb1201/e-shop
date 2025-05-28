package hr.fer.dipl.controller;

import hr.fer.dipl.dto.CartItemDTO;
import hr.fer.dipl.service.CartServiceImpl;
import hr.fer.dipl.service.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping
public class CartController {

    private final CartServiceImpl cartService;

    @Autowired
    public CartController(CartServiceImpl service) {
        this.cartService = service;
    }

    // --- Cart Management ---
    @GetMapping("/cart")
    public ResponseEntity<List<CartItemDTO>> getCart() {
        return ResponseEntity.ok(cartService.getCartItems());
    }

    @PostMapping("/cart")
    public ResponseEntity<CartItemDTO> addToCart(@RequestBody CartItemDTO cartItemDTO) {
        CartItemDTO createdItem = cartService.addToCart(cartItemDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @DeleteMapping("/cart/{cartItemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long cartItemId) {
        cartService.removeFromCart(cartItemId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/cart/{cartItemId}")
    public ResponseEntity<CartItemDTO> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestBody CartItemDTO cartItemDTO) {
        CartItemDTO updatedItem = cartService.updateCartItem(cartItemId, cartItemDTO);
        return ResponseEntity.ok(updatedItem);
    }

    @DeleteMapping("/cart")
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok().build();
    }
}
