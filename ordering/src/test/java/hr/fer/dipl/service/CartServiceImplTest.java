package hr.fer.dipl.service;

import hr.fer.dipl.client.catalog.ProductCatalogClient;
import hr.fer.dipl.client.catalog.model.ProductDTO;
import hr.fer.dipl.client.inventory.InventoryClient;
import hr.fer.dipl.client.inventory.model.InventoryRequest;
import hr.fer.dipl.db.model.CartItem;
import hr.fer.dipl.db.repository.CartItemRepository;
import hr.fer.dipl.dto.CartItemDTO;
import hr.fer.dipl.exception.InsufficientInventoryException;
import hr.fer.dipl.exception.InventoryReservationException;
import hr.fer.dipl.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private InventoryClient inventoryClient;
    @Mock private SecurityUtils securityUtils;
    @Mock private ProductCatalogClient catalogClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private static final Long USER_ID = 42L;
    private static final Long PRODUCT_ID = 100L;

    private CartItem existingCartItem;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        existingCartItem = new CartItem();
        existingCartItem.setId(1L);
        existingCartItem.setUserId(USER_ID);
        existingCartItem.setProductId(PRODUCT_ID);
        existingCartItem.setProductName("Widget");
        existingCartItem.setPrice(BigDecimal.valueOf(9.99));
        existingCartItem.setQuantity(3);
        existingCartItem.setCreatedAt(LocalDateTime.now());
        existingCartItem.setUpdatedAt(LocalDateTime.now());

        productDTO = new ProductDTO(100, "Widget", "Electronics",
                BigDecimal.valueOf(9.99), BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ getCartItems

    @Test
    void getCartItems_returnsMappedDTOs() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(existingCartItem));

        List<CartItemDTO> result = cartService.getCartItems();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(result.getFirst().getQuantity()).isEqualTo(3);
    }

    @Test
    void getCartItems_returnsEmptyList_whenCartIsEmpty() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertThat(cartService.getCartItems()).isEmpty();
    }

    // ------------------------------------------------------------------ addToCart â€” new item

    @Test
    void addToCart_createsNewItem_whenProductNotYetInCart() {
        CartItemDTO dto = buildCartItemDTO(PRODUCT_ID, 2);

        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryClient.checkAvailability(PRODUCT_ID, 2)).thenReturn(true);
        when(catalogClient.getProductById(PRODUCT_ID)).thenReturn(productDTO);
        when(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(null);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(existingCartItem);

        CartItemDTO result = cartService.addToCart(dto);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(PRODUCT_ID);
        verify(inventoryClient).reserveInventory(eq(PRODUCT_ID), any(InventoryRequest.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addToCart_incrementsQuantity_whenProductAlreadyInCart() {
        CartItemDTO dto = buildCartItemDTO(PRODUCT_ID, 2);

        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryClient.checkAvailability(PRODUCT_ID, 2)).thenReturn(true);
        when(catalogClient.getProductById(PRODUCT_ID)).thenReturn(productDTO);
        when(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(existingCartItem);
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        cartService.addToCart(dto);

        assertThat(existingCartItem.getQuantity()).isEqualTo(5);  // 3 + 2
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    void addToCart_throws_whenInventoryNotAvailable() {
        CartItemDTO dto = buildCartItemDTO(PRODUCT_ID, 10);

        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryClient.checkAvailability(PRODUCT_ID, 10)).thenReturn(false);

        assertThatThrownBy(() -> cartService.addToCart(dto))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("not available");

        verify(inventoryClient, never()).reserveInventory(any(), any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addToCart_throws_whenInventoryReservationFails() {
        CartItemDTO dto = buildCartItemDTO(PRODUCT_ID, 2);

        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryClient.checkAvailability(PRODUCT_ID, 2)).thenReturn(true);
        when(catalogClient.getProductById(PRODUCT_ID)).thenReturn(productDTO);
       // when(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(null);
        doThrow(new RuntimeException("inventory service unavailable"))
                .when(inventoryClient).reserveInventory(eq(PRODUCT_ID), any(InventoryRequest.class));

        assertThatThrownBy(() -> cartService.addToCart(dto))
                .isInstanceOf(InventoryReservationException.class)
                .hasMessageContaining("Failed to reserve");
    }

    // ------------------------------------------------------------------ removeFromCart

    @Test
    void removeFromCart_releasesReservationAndDeletesItem() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingCartItem));
        doNothing().when(inventoryClient).releaseReservation(anyLong(), any(InventoryRequest.class));
        doNothing().when(cartItemRepository).delete(existingCartItem);

        cartService.removeFromCart(1L);

        verify(inventoryClient).releaseReservation(eq(PRODUCT_ID), any(InventoryRequest.class));
        verify(cartItemRepository).delete(existingCartItem);
    }

    @Test
    void removeFromCart_throws_whenCartItemNotFound() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart item not found");

        verify(inventoryClient, never()).releaseReservation(any(), any());
    }

    @Test
    void removeFromCart_throws_whenReleaseReservationFails() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingCartItem));
        doThrow(new RuntimeException("inventory down"))
                .when(inventoryClient).releaseReservation(anyLong(), any(InventoryRequest.class));

        assertThatThrownBy(() -> cartService.removeFromCart(1L))
                .isInstanceOf(InventoryReservationException.class)
                .hasMessageContaining("Failed to release");
    }

    // ------------------------------------------------------------------ updateCartItem

    @Test
    void updateCartItem_releasesInventory_whenQuantityDecreases() {
        CartItemDTO updateDTO = buildCartItemDTO(PRODUCT_ID, 1);  // old=3, new=1 â†’ release 2
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingCartItem));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        cartService.updateCartItem(1L, updateDTO);

        verify(inventoryClient).releaseReservation(eq(PRODUCT_ID),
                argThat(r -> r.getQuantity() == 2));
        assertThat(existingCartItem.getQuantity()).isEqualTo(1);
    }

    @Test
    void updateCartItem_reservesMoreInventory_whenQuantityIncreases() {
        CartItemDTO updateDTO = buildCartItemDTO(PRODUCT_ID, 5);  // old=3, new=5 â†’ reserve 2
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingCartItem));
        when(inventoryClient.checkAvailability(PRODUCT_ID, 2)).thenReturn(true);
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        cartService.updateCartItem(1L, updateDTO);

        verify(inventoryClient).checkAvailability(PRODUCT_ID, 2);
        verify(inventoryClient).reserveInventory(eq(PRODUCT_ID),
                argThat(r -> r.getQuantity() == 2));
        assertThat(existingCartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    void updateCartItem_noInventoryCall_whenQuantityUnchanged() {
        CartItemDTO updateDTO = buildCartItemDTO(PRODUCT_ID, 3);  // old=3, new=3 â†’ no-op
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingCartItem));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        cartService.updateCartItem(1L, updateDTO);

        verify(inventoryClient, never()).checkAvailability(any(), anyInt());
        verify(inventoryClient, never()).reserveInventory(any(), any());
        verify(inventoryClient, never()).releaseReservation(any(), any());
    }

    @Test
    void updateCartItem_throws_whenInsufficientInventoryOnIncrease() {
        CartItemDTO updateDTO = buildCartItemDTO(PRODUCT_ID, 10);  // old=3, new=10 â†’ reserve 7
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingCartItem));
        when(inventoryClient.checkAvailability(PRODUCT_ID, 7)).thenReturn(false);

        assertThatThrownBy(() -> cartService.updateCartItem(1L, updateDTO))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient inventory");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateCartItem_throws_whenCartItemNotFound() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateCartItem(99L, buildCartItemDTO(PRODUCT_ID, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart item not found");
    }

    @Test
    void updateCartItem_throws_whenInventoryReservationFailsOnIncrease() {
        CartItemDTO updateDTO = buildCartItemDTO(PRODUCT_ID, 5);  // old=3, new=5 â†’ reserve 2
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existingCartItem));
        when(inventoryClient.checkAvailability(PRODUCT_ID, 2)).thenReturn(true);
        doThrow(new RuntimeException("timeout"))
                .when(inventoryClient).reserveInventory(eq(PRODUCT_ID), any(InventoryRequest.class));

        assertThatThrownBy(() -> cartService.updateCartItem(1L, updateDTO))
                .isInstanceOf(InventoryReservationException.class)
                .hasMessageContaining("Failed to update");
    }

    // ------------------------------------------------------------------ clearCart / clearCartForUser

    @Test
    void clearCart_delegatesToClearCartForUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        doNothing().when(cartItemRepository).deleteByUserId(USER_ID);

        cartService.clearCart();

        verify(cartItemRepository).deleteByUserId(USER_ID);
    }

    @Test
    void clearCartForUser_deletesAllItemsForUser() {
        doNothing().when(cartItemRepository).deleteByUserId(USER_ID);

        cartService.clearCartForUser(USER_ID);

        verify(cartItemRepository).deleteByUserId(USER_ID);
    }

    // ------------------------------------------------------------------ getCartItemsForUser

    @Test
    void getCartItemsForUser_returnsRawEntities() {
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(existingCartItem));

        List<CartItem> result = cartService.getCartItemsForUser(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(USER_ID);
    }

    // ------------------------------------------------------------------ helpers

    private CartItemDTO buildCartItemDTO(Long productId, int quantity) {
        CartItemDTO dto = new CartItemDTO();
        dto.setProductId(productId);
        dto.setProductName("Widget");
        dto.setQuantity(quantity);
        return dto;
    }
}
