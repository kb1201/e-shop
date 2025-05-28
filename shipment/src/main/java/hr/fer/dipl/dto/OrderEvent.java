package hr.fer.dipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private Long orderId;
    private String shippingAddress;
    private String billingAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
