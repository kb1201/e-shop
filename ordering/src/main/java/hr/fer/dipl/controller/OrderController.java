package hr.fer.dipl.controller;

import hr.fer.dipl.dto.CartItemDTO;
import hr.fer.dipl.dto.OrderDTO;
import hr.fer.dipl.dto.OrderRequest;
import hr.fer.dipl.service.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class OrderController {

    private final OrderServiceImpl orderService;

    @Autowired
    public OrderController(OrderServiceImpl orderService) {
        this.orderService = orderService;
    }



    // --- Order Checkout & Management ---
    @PostMapping("/orders")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderRequest orderRequest) {
        return new ResponseEntity<>(orderService.createOrder(orderRequest), HttpStatus.CREATED);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long orderId, @RequestBody OrderRequest orderRequest) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, orderRequest.getStatus()));
    }

}