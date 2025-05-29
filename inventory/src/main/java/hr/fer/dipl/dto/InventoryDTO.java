package hr.fer.dipl.dto;

import hr.fer.dipl.db.model.InventoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private Integer quantityAvailable;
    private Integer reservedQuantity;
    private Integer reorderThreshold;
    private String warehouseLocation;
    private String shelfLocation;
    private InventoryStatus status;
    private LocalDateTime lastUpdated;
}
