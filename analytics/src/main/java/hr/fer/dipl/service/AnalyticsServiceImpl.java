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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AnalyticsServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OrderAnalyticsDTO getOrderLast30DaysAnalytics() {
        String sql = """
                SELECT count(*)                                                                  as total_orders,
                       countIf(status = 'PENDING')                                               as pending_orders,
                       countIf(status = 'PAID')                                                  as paid_orders,
                       countIf(status = 'PROCESSING')                                            as processing_orders,
                       countIf(status = 'SHIPPED')                                             as shipped_orders,
                       countIf(status = 'DELIVERED')                                             as delivered_orders,
                       countIf(status = 'CANCELLED')                                             as cancelled_orders,
                       countIf(status = 'REFUNDED')                                              as refunded_orders,
                       sum(total_amount)                                                         as total_order_value,
                       sumIf(total_amount, status = 'DELIVERED')                                 as delivered_revenue,
                       sumIf(total_amount, status IN ('CANCELLED', 'REFUNDED'))                  as lost_revenue,
                       avg(total_amount)                                                         as avg_order_value,
                       avgIf(total_amount, status = 'DELIVERED')                                 as avg_delivered_order_value,
                       round(countIf(status = 'DELIVERED') * 100.0 / count(*), 2)                as delivery_rate_pct,
                       round(countIf(status IN ('CANCELLED', 'REFUNDED')) * 100.0 / count(*), 2) as cancellation_rate_pct
                FROM orders_fact
                WHERE created_at >= now() - INTERVAL 30 DAY
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> OrderAnalyticsDTO.builder()
                .totalOrders(rs.getLong("total_orders"))
                .pendingOrders(rs.getLong("pending_orders"))
                .paidOrders(rs.getLong("paid_orders"))
                .processingOrders(rs.getLong("processing_orders"))
                .shippedOrders(rs.getLong("shipped_orders"))
                .deliveredOrders(rs.getLong("delivered_orders"))
                .cancelledOrders(rs.getLong("cancelled_orders"))
                .refundedOrders(rs.getLong("refunded_orders"))
                .totalOrderValue(rs.getDouble("total_order_value"))
                .deliveredRevenue(rs.getDouble("delivered_revenue"))
                .lostRevenue(rs.getDouble("lost_revenue"))
                .avgOrderValue(rs.getDouble("avg_order_value"))
                .avgDeliveredOrderValue(rs.getDouble("avg_delivered_order_value"))
                .deliveryRatePct(rs.getDouble("delivery_rate_pct"))
                .cancellationRatePct(rs.getDouble("cancellation_rate_pct"))
                .build());
    }

    @Override
    public List<PaymentMethodAnalyticsDto> getPaymentMethodStats() {
        String sql = """
                    SELECT
                        payment_method,
                        count(*) as order_count,
                        sum(total_amount) as total_revenue,
                        avg(total_amount) as avg_order_value,
                        round(count(*) * 100.0 / sum(count(*)) OVER(), 2) as percentage_of_orders,
                        round(sum(total_amount) * 100.0 / sum(sum(total_amount)) OVER(), 2) as percentage_of_revenue
                    FROM orders_fact
                    WHERE created_at >= now() - INTERVAL 30 DAY
                    GROUP BY payment_method
                    ORDER BY order_count DESC
                """;

        RowMapper<PaymentMethodAnalyticsDto> rowMapper = (rs, rowNum) -> PaymentMethodAnalyticsDto.builder()
                .paymentMethod(rs.getString("payment_method"))
                .orderCount(rs.getLong("order_count"))
                .totalRevenue(rs.getDouble("total_revenue"))
                .avgOrderValue(rs.getDouble("avg_order_value"))
                .percentageOfOrders(rs.getDouble("percentage_of_orders"))
                .percentageOfRevenue(rs.getDouble("percentage_of_revenue"))
                .build();

        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<OrderStatusDurationDTO> fetchOrderStatusDurations() {
        String sql = """
                WITH order_status_events AS (
                    SELECT
                        id AS order_id,
                        status AS to_status,
                        updated_at,
                        lagInFrame(status, 1, status) OVER (PARTITION BY id ORDER BY updated_at ASC) AS from_status,
                        lagInFrame(updated_at, 1, updated_at) OVER (PARTITION BY id ORDER BY updated_at ASC) AS prev_updated_at
                    FROM orders_fact
                    WHERE created_at >= (now() - toIntervalDay(60))
                ),
                     status_durations AS (
                         SELECT
                             order_id,
                             from_status,
                             to_status,
                             dateDiff('minute', prev_updated_at, updated_at) AS duration_minutes
                         FROM order_status_events
                         WHERE from_status != to_status
                           AND dateDiff('minute', prev_updated_at, updated_at) > 0
                     )
                SELECT
                    from_status,
                    to_status,
                    count(*) AS transitions_count,
                    round(avg(duration_minutes) / 60, 2) AS avg_hours,
                    round(median(duration_minutes) / 60, 2) AS median_hours,
                    round(max(duration_minutes) / 60, 2) AS max_hours
                FROM status_durations
                GROUP BY from_status, to_status
                ORDER BY avg_hours DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> OrderStatusDurationDTO.builder()
                .fromStatus(rs.getString("from_status"))
                .toStatus(rs.getString("to_status"))
                .statusOccurrences(rs.getLong("transitions_count"))
                .avgHoursInStatus(rs.getDouble("avg_hours"))
                .medianHoursInStatus(rs.getDouble("median_hours"))
                .maxHoursInStatus(rs.getDouble("max_hours"))
                .build()
        );
    }

    @Override
    public InventoryHealthDTO fetchInventoryHealthSnapshot() {
        String sql = """
                WITH latest_inventory AS (
                    SELECT
                        product_id,
                        sku,
                        quantity_available,
                        reserved_quantity,
                        reorder_threshold,
                        warehouse_location,
                        status,
                        last_updated,
                        ROW_NUMBER() OVER (PARTITION BY product_id ORDER BY last_updated DESC) as rn
                    FROM inventory_fact
                )
                SELECT
                    countIf(status = 'IN_STOCK') as in_stock_products,
                    countIf(status = 'LOW_STOCK') as low_stock_products,
                    countIf(status = 'OUT_OF_STOCK') as out_of_stock_products,
                    countIf(status = 'DISCONTINUED') as discontinued_products,

                    sum(quantity_available) as total_available_qty,
                    sum(reserved_quantity) as total_reserved_qty,
                    avg(quantity_available) as avg_available_per_product,

                    round(countIf(status = 'IN_STOCK') * 100.0 / count(*), 2) as in_stock_rate_pct,
                    round(countIf(status = 'OUT_OF_STOCK') * 100.0 / count(*), 2) as out_of_stock_rate_pct,

                    countIf(quantity_available <= reorder_threshold AND status != 'DISCONTINUED') as products_need_reorder
                FROM latest_inventory
                WHERE rn = 1
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> InventoryHealthDTO.builder()
                .inStockProducts(rs.getLong("in_stock_products"))
                .lowStockProducts(rs.getLong("low_stock_products"))
                .outOfStockProducts(rs.getLong("out_of_stock_products"))
                .discontinuedProducts(rs.getLong("discontinued_products"))
                .totalAvailableQty(rs.getLong("total_available_qty"))
                .totalReservedQty(rs.getLong("total_reserved_qty"))
                .avgAvailablePerProduct(rs.getDouble("avg_available_per_product"))
                .inStockRatePct(rs.getDouble("in_stock_rate_pct"))
                .outOfStockRatePct(rs.getDouble("out_of_stock_rate_pct"))
                .productsNeedReorder(rs.getLong("products_need_reorder"))
                .build());
    }

    @Override
    public List<WarehouseInventoryStatsDTO> getWarehouseInventoryStats() {
        String sql = """
                WITH latest_inventory AS (
                    SELECT
                        product_id,
                        quantity_available,
                        reserved_quantity,
                        warehouse_location,
                        status,
                        last_updated,
                        ROW_NUMBER() OVER (PARTITION BY product_id ORDER BY last_updated DESC) as rn
                    FROM inventory_fact
                    WHERE warehouse_location IS NOT NULL
                )
                SELECT
                    warehouse_location,
                    count(*) as products_count,
                    sum(quantity_available) as total_available_qty,
                    sum(reserved_quantity) as total_reserved_qty,
                    round(avg(quantity_available), 2) as avg_qty_per_product,
                    countIf(status = 'IN_STOCK') as in_stock_count,
                    countIf(status = 'OUT_OF_STOCK') as out_of_stock_count,
                    round(countIf(status = 'IN_STOCK') * 100.0 / count(*), 2) as stock_health_pct
                FROM latest_inventory
                WHERE rn = 1
                GROUP BY warehouse_location
                ORDER BY total_available_qty DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> WarehouseInventoryStatsDTO.builder()
                .warehouseLocation(rs.getString("warehouse_location"))
                .productsCount(rs.getLong("products_count"))
                .totalAvailableQty(rs.getLong("total_available_qty"))
                .totalReservedQty(rs.getLong("total_reserved_qty"))
                .avgQtyPerProduct(rs.getDouble("avg_qty_per_product"))
                .inStockCount(rs.getLong("in_stock_count"))
                .outOfStockCount(rs.getLong("out_of_stock_count"))
                .stockHealthPct(rs.getDouble("stock_health_pct"))
                .build());
    }

    @Override
    public List<WeeklyInventoryStatsDTO> getWeeklyInventoryStats() {
        String sql = """
                    SELECT
                        toStartOfWeek(last_updated) as week_start,
                        sum(quantity_available) as total_available,
                        sum(reserved_quantity) as total_reserved,
                        avg(quantity_available) as avg_available_per_product,
                        countIf(status = 'OUT_OF_STOCK') as out_of_stock_count,
                        count(DISTINCT product_id) as products_tracked
                    FROM inventory_fact
                    WHERE last_updated >= now() - INTERVAL 4 WEEK
                    GROUP BY week_start
                    ORDER BY week_start ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> WeeklyInventoryStatsDTO.builder()
                .weekStart(rs.getDate("week_start").toLocalDate())
                .totalAvailable(rs.getLong("total_available"))
                .totalReserved(rs.getLong("total_reserved"))
                .avgAvailablePerProduct(rs.getDouble("avg_available_per_product"))
                .outOfStockCount(rs.getLong("out_of_stock_count"))
                .productsTracked(rs.getLong("products_tracked"))
                .build());
    }

    @Override
    public ShipmentAnalyticsDto getShipmentAnalytics() {
        String sql = """
                    SELECT
                        count(*) as total_shipments,
                        countIf(status = 'DELIVERED') as delivered_shipments,
                        countIf(status = 'IN_DELIVERY') as in_transit_shipments,
                        countIf(status = 'REJECTED') as rejected_shipments,
                        countIf(status = 'CREATED') as pending_shipments,

                        round(countIf(status = 'DELIVERED') * 100.0 / count(*), 2) as delivery_success_rate_pct,
                        round(countIf(status = 'REJECTED') * 100.0 / count(*), 2) as rejection_rate_pct,

                        round(avgIf(dateDiff('hour', created_at, updated_at), status = 'DELIVERED'), 2) as avg_delivery_time_hours,
                        round(medianIf(dateDiff('hour', created_at, updated_at), status = 'DELIVERED'), 2) as median_delivery_time_hours
                    FROM shipment_fact
                    WHERE created_at >= now() - INTERVAL 30 DAY
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> ShipmentAnalyticsDto.builder()
                .totalShipments(rs.getInt("total_shipments"))
                .deliveredShipments(rs.getInt("delivered_shipments"))
                .inTransitShipments(rs.getInt("in_transit_shipments"))
                .rejectedShipments(rs.getInt("rejected_shipments"))
                .pendingShipments(rs.getInt("pending_shipments"))
                .deliverySuccessRatePct(rs.getDouble("delivery_success_rate_pct"))
                .rejectionRatePct(rs.getDouble("rejection_rate_pct"))
                .avgDeliveryTimeHours(rs.getDouble("avg_delivery_time_hours"))
                .medianDeliveryTimeHours(rs.getDouble("median_delivery_time_hours"))
                .build());
    }

    @Override
    public List<ShipmentStatusDurationDTO> getShipmentStatusDurations() {
        String sql = """
            WITH shipment_transitions AS (
                SELECT
                    id,
                    status,
                    created_at,
                    updated_at,
                    lagInFrame(updated_at, 1, created_at) OVER (PARTITION BY id ORDER BY updated_at ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) as prev_time
                FROM shipment_fact
                WHERE created_at >= now() - INTERVAL 30 DAY
            ),
            shipment_durations AS (
                SELECT
                    id,
                    status,
                    created_at,
                    updated_at,
                    prev_time,
                    dateDiff('hour', prev_time, updated_at) as hours_in_status
                FROM shipment_transitions
            )
            SELECT
                status,
                count(*) as status_occurrences,
                round(avg(hours_in_status), 2) as avg_hours_in_status,
                round(median(hours_in_status), 2) as median_hours_in_status,
                quantile(0.95)(hours_in_status) as p95_hours_in_status
            FROM shipment_durations
            WHERE hours_in_status >= 0
            GROUP BY status
            ORDER BY
                CASE status
                    WHEN 'CREATED' THEN 1
                    WHEN 'IN_DELIVERY' THEN 2
                    WHEN 'DELIVERED' THEN 3
                    WHEN 'REJECTED' THEN 4
                END
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> ShipmentStatusDurationDTO.builder()
                .status(rs.getString("status"))
                .statusOccurrences(rs.getLong("status_occurrences"))
                .avgHoursInStatus(rs.getDouble("avg_hours_in_status"))
                .medianHoursInStatus(rs.getDouble("median_hours_in_status"))
                .p95HoursInStatus(rs.getDouble("p95_hours_in_status"))
                .build()
        );
    }

    @Override
    public CartAnalyticsDTO getCartAnalyticsSummary() {
        String sql = """
            WITH cart_summary AS (
                SELECT
                    user_id,
                    count(*) as items_in_cart,
                    sum(price * quantity) as cart_value,
                    max(updated_at) as last_cart_activity,
                    dateDiff('hour', max(updated_at), now()) as hours_since_last_activity
                FROM cart_items_fact
                GROUP BY user_id
            ),
            user_orders AS (
                SELECT DISTINCT user_id
                FROM orders_fact
                WHERE created_at >= now() - INTERVAL 7 DAY
            ),
            cart_with_orders AS (
                SELECT
                    cs.*,
                    CASE WHEN uo.user_id IS NOT NULL THEN 1 ELSE 0 END as has_recent_order
                FROM cart_summary cs
                LEFT JOIN user_orders uo ON cs.user_id = uo.user_id
                WHERE cs.hours_since_last_activity >= 0
            )
            SELECT
                count(*) as total_active_carts,
                sum(items_in_cart) as total_items_in_carts,
                sum(cart_value) as total_cart_value,
                round(avg(cart_value), 2) as avg_cart_value,
                round(avg(items_in_cart), 2) as avg_items_per_cart,

                countIf(hours_since_last_activity > 24) as carts_abandoned_24h,
                countIf(hours_since_last_activity > 72) as carts_abandoned_72h,
                countIf(hours_since_last_activity > 168) as carts_abandoned_1week,

                round(countIf(hours_since_last_activity > 24) * 100.0 / count(*), 2) as abandonment_rate_24h_pct,
                round(countIf(hours_since_last_activity > 72) * 100.0 / count(*), 2) as abandonment_rate_72h_pct,

                sumIf(cart_value, hours_since_last_activity > 24) as abandoned_cart_value_24h,
                sumIf(cart_value, hours_since_last_activity > 72) as abandoned_cart_value_72h,

                countIf(has_recent_order = 0) as users_with_cart_no_recent_order,
                round(countIf(has_recent_order = 0) * 100.0 / count(*), 2) as no_recent_order_rate_pct
            FROM cart_with_orders
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> CartAnalyticsDTO.builder()
                .totalActiveCarts(rs.getLong("total_active_carts"))
                .totalItemsInCarts(rs.getLong("total_items_in_carts"))
                .totalCartValue(rs.getBigDecimal("total_cart_value"))
                .avgCartValue(rs.getDouble("avg_cart_value"))
                .avgItemsPerCart(rs.getDouble("avg_items_per_cart"))
                .cartsAbandoned24h(rs.getLong("carts_abandoned_24h"))
                .cartsAbandoned72h(rs.getLong("carts_abandoned_72h"))
                .cartsAbandoned1week(rs.getLong("carts_abandoned_1week"))
                .abandonmentRate24hPct(rs.getDouble("abandonment_rate_24h_pct"))
                .abandonmentRate72hPct(rs.getDouble("abandonment_rate_72h_pct"))
                .abandonedCartValue24h(rs.getBigDecimal("abandoned_cart_value_24h"))
                .abandonedCartValue72h(rs.getBigDecimal("abandoned_cart_value_72h"))
                .usersWithCartNoRecentOrder(rs.getLong("users_with_cart_no_recent_order"))
                .noRecentOrderRatePct(rs.getDouble("no_recent_order_rate_pct"))
                .build()
        );
    }
}
