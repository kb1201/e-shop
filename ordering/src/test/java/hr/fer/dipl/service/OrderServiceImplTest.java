package hr.fer.dipl.service;

import hr.fer.dipl.client.inventory.InventoryClient;
import hr.fer.dipl.client.inventory.model.InventoryRequest;
import hr.fer.dipl.db.model.CartItem;
import hr.fer.dipl.db.model.Order;
import hr.fer.dipl.db.model.OrderItem;
import hr.fer.dipl.db.model.OrderStatus;
import hr.fer.dipl.db.repository.OrderItemRepository;
import hr.fer.dipl.db.repository.OrderRepository;
import hr.fer.dipl.dto.OrderDTO;
import hr.fer.dipl.dto.OrderRequest;
import hr.fer.dipl.exception.ResourceNotFoundException;
import hr.fer.dipl.service.kafka.OrderEventProducer;
import hr.fer.dipl.service.kafka.UserProductProducer;
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
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private InventoryClient inventoryClient;
    @Mock private SecurityUtils securityUtils;
    @Mock private OrderEventProducer orderEventProducer;
    @Mock private UserProductProducer userProductProducer;
    @Mock private CartService cartService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final Long USER_ID = 42L;

    private Order savedOrder;
    private OrderItem savedOrderItem;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setUserId(USER_ID);
        cartItem.setProductId(100L);
        cartItem.setProductName("Widget");
        cartItem.setPrice(BigDecimal.valueOf(10.00));
        cartItem.setQuantity(2);

        savedOrder = new Order();
        savedOrder.setId(10L);
        savedOrder.setUserId(USER_ID);
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setShippingAddress("123 Main St");
        savedOrder.setBillingAddress("123 Main St");
        savedOrder.setPaymentMethod("CARD");
        savedOrder.setTotalAmount(BigDecimal.valueOf(20.00));
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setUpdatedAt(LocalDateTime.now());

        savedOrderItem = new OrderItem();
        savedOrderItem.setId(1L);
        savedOrderItem.setOrder(savedOrder);
        savedOrderItem.setProductId(100L);
        savedOrderItem.setProductName("Widget");
        savedOrderItem.setQuantity(2);
        savedOrderItem.setUnitPrice(BigDecimal.valueOf(10.00));
        savedOrderItem.setCreatedAt(LocalDateTime.now());
    }

    // ------------------------------------------------------------------ createOrder

    @Test
    void createOrder_savesOrderAndReturnsDTO() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartService.getCartItemsForUser(USER_ID)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(savedOrderItem);
        doNothing().when(inventoryClient).commitReservation(anyLong(), any(InventoryRequest.class));
        doNothing().when(cartService).clearCartForUser(USER_ID);
        doNothing().when(orderEventProducer).sendOrderEvent(any(OrderDTO.class));
        doNothing().when(userProductProducer).sendUserProductEvent(any(OrderDTO.class));

        OrderDTO result = orderService.createOrder(buildOrderRequest());

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        verify(inventoryClient).commitReservation(eq(100L), any(InventoryRequest.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(cartService).clearCartForUser(USER_ID);
        verify(orderEventProducer).sendOrderEvent(any(OrderDTO.class));
    }

    @Test
    void createOrder_calculatesTotalAmountFromCartItems() {
        CartItem item1 = buildCartItem(1L, 101L, BigDecimal.valueOf(5.00), 3);   // 15
        CartItem item2 = buildCartItem(2L, 102L, BigDecimal.valueOf(10.00), 2);  // 20

        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartService.getCartItemsForUser(USER_ID)).thenReturn(List.of(item1, item2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(savedOrderItem);
        doNothing().when(inventoryClient).commitReservation(anyLong(), any());
        doNothing().when(cartService).clearCartForUser(any());
        doNothing().when(orderEventProducer).sendOrderEvent(any());
        doNothing().when(userProductProducer).sendUserProductEvent(any());

        OrderDTO result = orderService.createOrder(buildOrderRequest());

        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(35.00));
    }

    @Test
    void createOrder_throws_whenCartIsEmpty() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartService.getCartItemsForUser(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrder(buildOrderRequest()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void createOrder_wrapsException_whenInventoryCommitFails() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(cartService.getCartItemsForUser(USER_ID)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doThrow(new RuntimeException("inventory down"))
                .when(inventoryClient).commitReservation(anyLong(), any(InventoryRequest.class));

        assertThatThrownBy(() -> orderService.createOrder(buildOrderRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create order");
    }

    // ------------------------------------------------------------------ getOrderById

    @Test
    void getOrderById_returnsDTO_whenOrderBelongsToUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(orderRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(savedOrderItem));

        OrderDTO result = orderService.getOrderById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void getOrderById_throws_whenOrderNotFound() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(orderRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    // ------------------------------------------------------------------ getAllOrders

    @Test
    void getAllOrders_returnsDTOsForAllOrders() {
        Order order2 = new Order();
        order2.setId(20L);
        order2.setUserId(99L);
        order2.setStatus(OrderStatus.PAID);
        order2.setTotalAmount(BigDecimal.valueOf(50.00));
        order2.setCreatedAt(LocalDateTime.now());
        order2.setUpdatedAt(LocalDateTime.now());

        when(orderRepository.findAll()).thenReturn(List.of(savedOrder, order2));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(savedOrderItem));
        when(orderItemRepository.findByOrderId(20L)).thenReturn(List.of());

        List<OrderDTO> result = orderService.getAllOrders();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(1).getId()).isEqualTo(20L);
    }

    @Test
    void getAllOrders_returnsEmptyList_whenNoOrders() {
        when(orderRepository.findAll()).thenReturn(List.of());

        assertThat(orderService.getAllOrders()).isEmpty();
    }

    // ------------------------------------------------------------------ getOrdersByUserId

    @Test
    void getOrdersByUserId_returnsOrders_whenUserMatchesCurrentUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(orderRepository.findByUserId(USER_ID)).thenReturn(List.of(savedOrder));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(savedOrderItem));

        List<OrderDTO> result = orderService.getOrdersByUserId(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void getOrdersByUserId_throws_whenRequestingAnotherUsersOrders() {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);

        assertThatThrownBy(() -> orderService.getOrdersByUserId(999L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Unauthorized");
    }

    // ------------------------------------------------------------------ updateOrderStatus

    @Test
    void updateOrderStatus_updatesAndReturnsDTO() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(savedOrder)).thenReturn(savedOrder);
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(savedOrderItem));

        OrderDTO result = orderService.updateOrderStatus(10L, OrderStatus.SHIPPED);

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(result.getItems()).hasSize(1);
        verify(orderRepository).save(savedOrder);
    }

    @Test
    void updateOrderStatus_throws_whenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, OrderStatus.SHIPPED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    // ------------------------------------------------------------------ helpers

    private OrderRequest buildOrderRequest() {
        OrderRequest req = new OrderRequest();
        req.setShippingAddress("123 Main St");
        req.setBillingAddress("123 Main St");
        req.setPaymentMethod("CARD");
        return req;
    }

    private CartItem buildCartItem(Long id, Long productId, BigDecimal price, int quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(USER_ID);
        item.setProductId(productId);
        item.setProductName("Product " + productId);
        item.setPrice(price);
        item.setQuantity(quantity);
        return item;
    }
}
