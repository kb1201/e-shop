package hr.fer.dipl.service;

import hr.fer.dipl.client.catalog.ProductCatalogClient;
import hr.fer.dipl.client.inventory.InventoryClient;
import hr.fer.dipl.client.inventory.model.InventoryRequest;
import hr.fer.dipl.db.model.CartItem;
import hr.fer.dipl.db.repository.CartItemRepository;
import hr.fer.dipl.dto.CartItemDTO;
import hr.fer.dipl.exception.InsufficientInventoryException;
import hr.fer.dipl.exception.InventoryReservationException;
import hr.fer.dipl.exception.ResourceNotFoundException;
import hr.fer.dipl.mapper.DtoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl  {

    private final CartItemRepository cartItemRepository;
    private final InventoryClient inventoryClient;
    private final SecurityUtils securityUtils;
    private final ProductCatalogClient catalogClient;

    @Autowired
    public CartServiceImpl(
            CartItemRepository cartItemRepository,
            InventoryClient inventoryClient,
            SecurityUtils securityUtils,
            ProductCatalogClient catalogClient) {
        this.cartItemRepository = cartItemRepository;
        this.inventoryClient = inventoryClient;
        this.securityUtils = securityUtils;
        this.catalogClient = catalogClient;
    }

    public List<CartItemDTO> getCartItems() {
        Long userId = securityUtils.getCurrentUserId();
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        return cartItems.stream()
                .map(DtoMapper::toCartItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CartItemDTO addToCart(CartItemDTO cartItemDTO) {
        Long userId = securityUtils.getCurrentUserId();
        Long productId = cartItemDTO.getProductId();
        int quantity = cartItemDTO.getQuantity();

        boolean isAvailable = inventoryClient.checkAvailability(productId, quantity);
        if (!isAvailable) {
            throw new InsufficientInventoryException("Product is not available in the requested quantity");
        }

        var price = catalogClient.getProductById(productId).getActualPrice();

        try {
            inventoryClient.reserveInventory(productId, new InventoryRequest(quantity));

            // Check if product already exists in cart
            CartItem existingItem = cartItemRepository.findByUserIdAndProductId(userId, productId);
            CartItem item;

            if (existingItem != null) {
                // Update existing item
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                existingItem.setUpdatedAt(LocalDateTime.now());
                item = cartItemRepository.save(existingItem);
            } else {
                // Create new cart item
                CartItem newItem = new CartItem();
                newItem.setUserId(userId);
                newItem.setProductId(productId);
                newItem.setProductName(cartItemDTO.getProductName());
                newItem.setPrice(price);
                newItem.setQuantity(quantity);
                newItem.setCreatedAt(LocalDateTime.now());
                newItem.setUpdatedAt(LocalDateTime.now());
                item = cartItemRepository.save(newItem);
            }

            return DtoMapper.toCartItemDTO(item);
        } catch (Exception e) {
            throw new InventoryReservationException("Failed to reserve inventory: " + e.getMessage());
        }
    }

    @Transactional
    public void removeFromCart(Long cartItemId) {
        Long userId = securityUtils.getCurrentUserId();
        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        try {
            inventoryClient.releaseReservation(cartItem.getProductId(),
                    new InventoryRequest(cartItem.getQuantity()));
            cartItemRepository.delete(cartItem);
        } catch (Exception e) {
            throw new InventoryReservationException("Failed to release inventory reservation: " + e.getMessage());
        }
    }

    @Transactional
    public CartItemDTO updateCartItem(Long cartItemId, CartItemDTO cartItemDTO) {
        Long userId = securityUtils.getCurrentUserId();
        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        int oldQuantity = cartItem.getQuantity();
        int newQuantity = cartItemDTO.getQuantity();
        int quantityDifference = newQuantity - oldQuantity;

        try {
            // If reducing quantity, release inventory
            if (quantityDifference < 0) {
                inventoryClient.releaseReservation(cartItem.getProductId(),
                        new InventoryRequest(Math.abs(quantityDifference)));
            }
            // If increasing quantity, check availability and reserve
            else if (quantityDifference > 0) {
                boolean isAvailable = inventoryClient.checkAvailability(
                        cartItem.getProductId(), quantityDifference);

                if (!isAvailable) {
                    throw new InsufficientInventoryException("Insufficient inventory for requested quantity");
                }

                inventoryClient.reserveInventory(cartItem.getProductId(),
                        new InventoryRequest(quantityDifference));
            }

            cartItem.setQuantity(newQuantity);
            cartItem.setUpdatedAt(LocalDateTime.now());
            CartItem updatedItem = cartItemRepository.save(cartItem);

            return DtoMapper.toCartItemDTO(updatedItem);
        } catch (InsufficientInventoryException e) {
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            throw new InventoryReservationException("Failed to update inventory reservation: " + e.getMessage());
        }
    }

    @Transactional
    public void clearCart() {
        Long userId = securityUtils.getCurrentUserId();
        clearCartForUser(userId);
    }

    public List<CartItem> getCartItemsForUser(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    @Transactional
    public void clearCartForUser(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}