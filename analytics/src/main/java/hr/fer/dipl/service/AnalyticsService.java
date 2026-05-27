package hr.fer.dipl.service;

import hr.fer.dipl.dto.CartAnalyticsDTO;
import hr.fer.dipl.dto.InventoryHealthDTO;
import hr.fer.dipl.dto.OrderAnalyticsDTO;
import hr.fer.dipl.dto.OrderStatusDurationDTO;
import hr.fer.dipl.dto.PaymentMethodAnalyticsDto;
import hr.fer.dipl.dto.ShipmentAnalyticsDto;
import hr.fer.dipl.dto.ShipmentStatusDurationDTO;
import hr.fer.dipl.dto.WarehouseInventoryStatsDTO;
import hr.fer.dipl.dto.WeeklyInventoryStatsDTO;

import java.util.List;

public interface AnalyticsService {

    OrderAnalyticsDTO getOrderLast30DaysAnalytics();

    List<PaymentMethodAnalyticsDto> getPaymentMethodStats();

    List<OrderStatusDurationDTO> fetchOrderStatusDurations();

    InventoryHealthDTO fetchInventoryHealthSnapshot();

    List<WarehouseInventoryStatsDTO> getWarehouseInventoryStats();

    List<WeeklyInventoryStatsDTO> getWeeklyInventoryStats();

    ShipmentAnalyticsDto getShipmentAnalytics();

    List<ShipmentStatusDurationDTO> getShipmentStatusDurations();

    CartAnalyticsDTO getCartAnalyticsSummary();
}
