package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentMethodAnalyticsDto {
    private String paymentMethod;
    private long orderCount;
    private double totalRevenue;
    private double avgOrderValue;
    private double percentageOfOrders;
    private double percentageOfRevenue;
}
