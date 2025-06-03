package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryHealthDTO {
    private long inStockProducts;
    private long lowStockProducts;
    private long outOfStockProducts;
    private long discontinuedProducts;

    private long totalAvailableQty;
    private long totalReservedQty;
    private double avgAvailablePerProduct;

    private double inStockRatePct;
    private double outOfStockRatePct;

    private long productsNeedReorder;
}
