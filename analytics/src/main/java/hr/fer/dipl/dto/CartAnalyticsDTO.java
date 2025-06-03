package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CartAnalyticsDTO {
    // Cart metrics
    private long totalActiveCarts;
    private long totalItemsInCarts;
    private BigDecimal totalCartValue;
    private double avgCartValue;
    private double avgItemsPerCart;

    // Abandonment counts
    private long cartsAbandoned24h;
    private long cartsAbandoned72h;
    private long cartsAbandoned1week;

    // Abandonment rates (%)
    private double abandonmentRate24hPct;
    private double abandonmentRate72hPct;

    // Potential revenue impact
    private BigDecimal abandonedCartValue24h;
    private BigDecimal abandonedCartValue72h;

    // Conversion opportunity
    private long usersWithCartNoRecentOrder;
    private double noRecentOrderRatePct;
}
