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
import hr.fer.dipl.dto.PaymentDetails;
import hr.fer.dipl.exception.ResourceNotFoundException;
import hr.fer.dipl.mapper.DtoMapper;
import hr.fer.dipl.service.kafka.OrderEventProducer;
import hr.fer.dipl.service.kafka.UserProductProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryClient inventoryClient;
    private final SecurityUtils securityUtils;
    private final OrderEventProducer orderEventProducer;

    private final UserProductProducer userProductProducer;
    private final CartServiceImpl cartService;

    @Autowired
    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            InventoryClient inventoryClient,
            SecurityUtils securityUtils,
            OrderEventProducer orderEventProducer,
            UserProductProducer userProductProducer,
            CartServiceImpl cartService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryClient = inventoryClient;
        this.securityUtils = securityUtils;
        this.orderEventProducer = orderEventProducer;
        this.userProductProducer = userProductProducer;
        this.cartService = cartService;
    }

    @Transactional
    public OrderDTO createOrder(OrderRequest orderRequest) {
        Long userId = securityUtils.getCurrentUserId();
        List<CartItem> cartItems = cartService.getCartItemsForUser(userId);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create an order with an empty cart");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setBillingAddress(orderRequest.getBillingAddress());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setTotalAmount(calculateTotalAmount(cartItems));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        List<OrderItem> orderItems = new ArrayList<>();

        try {
            for (CartItem item : cartItems) {
                inventoryClient.commitReservation(item.getProductId(),
                        new InventoryRequest(item.getQuantity()));

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setProductId(item.getProductId());
                orderItem.setProductName(item.getProductName());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setUnitPrice(item.getPrice());
                orderItem.setCreatedAt(LocalDateTime.now());

                orderItems.add(orderItemRepository.save(orderItem));
            }

            processPayment(savedOrder, orderRequest.getPaymentDetails());
            cartService.clearCartForUser(userId);

            var dto = DtoMapper.toOrderDTO(savedOrder, orderItems);
            orderEventProducer.sendOrderEvent(dto);

            sendUserProductEventAsync( dto);

            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create order: " + e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendUserProductEventAsync(OrderDTO orderDTO) {
        userProductProducer.sendUserProductEvent(orderDTO);
    }

    public OrderDTO getOrderById(Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return DtoMapper.toOrderDTO(order, orderItems);
    }

    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        List<OrderDTO> orderDTOs = new ArrayList<>();

        for (Order order : orders) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
            orderDTOs.add(DtoMapper.toOrderDTO(order, orderItems));
        }

        return orderDTOs;
    }

    public List<OrderDTO> getOrdersByUserId(Long userId) {
        if (!securityUtils.getCurrentUserId().equals(userId)) {
            throw new SecurityException("Unauthorized access");
        }

        List<Order> orders = orderRepository.findByUserId(userId);
        List<OrderDTO> orderDTOs = new ArrayList<>();

        for (Order order : orders) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
            orderDTOs.add(DtoMapper.toOrderDTO(order, orderItems));
        }

        return orderDTOs;
    }


    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        return DtoMapper.toOrderDTO(updatedOrder, orderItems);
    }

    private void processPayment(Order order, PaymentDetails paymentDetails) {
        // In a real implementation, this would integrate with a payment processor
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    private BigDecimal calculateTotalAmount(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}