package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentAnalyticsDto {
    private int totalShipments;
    private int deliveredShipments;
    private int inTransitShipments;
    private int rejectedShipments;
    private int pendingShipments;

    private double deliverySuccessRatePct;
    private double rejectionRatePct;

    private double avgDeliveryTimeHours;
    private double medianDeliveryTimeHours;
}
