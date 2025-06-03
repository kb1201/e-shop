package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WarehouseInventoryStatsDTO {
    private String warehouseLocation;
    private long productsCount;
    private long totalAvailableQty;
    private long totalReservedQty;
    private double avgQtyPerProduct;
    private long inStockCount;
    private long outOfStockCount;
    private double stockHealthPct;
}
