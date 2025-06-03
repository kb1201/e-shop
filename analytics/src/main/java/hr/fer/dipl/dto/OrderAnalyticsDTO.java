package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderAnalyticsDTO {
    private long totalOrders;
    private long pendingOrders;
    private long paidOrders;
    private long processingOrders;
    private long shippedOrders;

    private long deliveredOrders;
    private long cancelledOrders;
    private long refundedOrders;

    private double totalOrderValue;
    private double deliveredRevenue;
    private double lostRevenue;

    private double avgOrderValue;
    private double avgDeliveredOrderValue;

    private double deliveryRatePct;
    private double cancellationRatePct;
}
