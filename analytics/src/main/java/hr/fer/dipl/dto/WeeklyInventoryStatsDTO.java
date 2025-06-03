package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class WeeklyInventoryStatsDTO {
    private LocalDate weekStart;
    private long totalAvailable;
    private long totalReserved;
    private double avgAvailablePerProduct;
    private long outOfStockCount;
    private long productsTracked;
}
