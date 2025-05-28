package hr.fer.dipl.client.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDTO {
    private Long id;
    private Long productId;
    private String sku;
    private int quantity;
    private int reservedQuantity;
    private int availableQuantity;
    private int reorderThreshold;
    private InventoryStatus status;
}