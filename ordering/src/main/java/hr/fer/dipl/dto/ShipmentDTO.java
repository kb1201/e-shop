package hr.fer.dipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDTO {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ShipmentStatus status;
    private OrderEvent order;
    public enum ShipmentStatus {
        CREATED,
        IN_DELIVERY,
        DELIVERED,
        REJECTED
    }
}
