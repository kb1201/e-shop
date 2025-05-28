package hr.fer.dipl.dto;

import hr.fer.dipl.db.model.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String billingAddress;
    private String paymentMethod;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}