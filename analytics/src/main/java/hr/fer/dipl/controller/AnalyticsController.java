package hr.fer.dipl.controller;


import hr.fer.dipl.dto.*;
import hr.fer.dipl.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;


    @GetMapping("/order/summary")
    public ResponseEntity<OrderAnalyticsDTO> getOrderLast30DaysAnalytics() {
        var metrics = analyticsService.getOrderLast30DaysAnalytics();
        return ResponseEntity.ok(metrics);
    }


    @GetMapping("/order/status")
    public ResponseEntity<List<OrderStatusDurationDTO>> fetchOrderStatusDurations() {
        var metrics = analyticsService.fetchOrderStatusDurations();
        return ResponseEntity.ok(metrics);
    }


    @GetMapping("/order/payment")
    public ResponseEntity<List<PaymentMethodAnalyticsDto>> getPaymentMethodStats() {
        var metrics = analyticsService.getPaymentMethodStats();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/inventory/summary")
    public ResponseEntity<InventoryHealthDTO> fetchInventoryHealthSnapshot() {
        var metrics = analyticsService.fetchInventoryHealthSnapshot();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/inventory/warehouse")
    public ResponseEntity<List<WarehouseInventoryStatsDTO>> getWarehouseInventoryStats() {
        var metrics = analyticsService.getWarehouseInventoryStats();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/inventory/weekly")
    public ResponseEntity<List<WeeklyInventoryStatsDTO>> getWeeklyInventoryStats() {
        var metrics = analyticsService.getWeeklyInventoryStats();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/shipment/summary")
    public ResponseEntity<ShipmentAnalyticsDto> getShipmentAnalytics() {
        var metrics = analyticsService.getShipmentAnalytics();
        return ResponseEntity.ok(metrics);
    }


    @GetMapping("/shipment/status-duration")
    public ResponseEntity<List<ShipmentStatusDurationDTO>> getShipmentStatusDurations() {
        var metrics = analyticsService.getShipmentStatusDurations();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/cart/summary")
    public ResponseEntity<CartAnalyticsDTO> getCartAnalyticsSummary() {
        var metrics = analyticsService.getCartAnalyticsSummary();
        return ResponseEntity.ok(metrics);
    }

}
