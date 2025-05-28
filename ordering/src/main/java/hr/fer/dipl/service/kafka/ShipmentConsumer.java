package hr.fer.dipl.service.kafka;

import hr.fer.dipl.db.model.OrderStatus;
import hr.fer.dipl.dto.ShipmentDTO;
import hr.fer.dipl.service.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShipmentConsumer {

    private final OrderServiceImpl orderService;

    //@Autowired
    public ShipmentConsumer(OrderServiceImpl orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "${spring.kafka.consumer.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeShipmentEvent(ShipmentDTO shipmentDTO) {
        log.info("Received shipment update event: {}", shipmentDTO);

        try {
            Long orderId = shipmentDTO.getOrder().getOrderId();
            ShipmentDTO.ShipmentStatus shipmentStatus = shipmentDTO.getStatus();
            OrderStatus newOrderStatus = mapShipmentStatusToOrderStatus(shipmentStatus);

            if (newOrderStatus != null) {
                log.info("Updating order {} status to {}", orderId, newOrderStatus);
                orderService.updateOrderStatus(orderId, newOrderStatus);
            } else {
                log.warn("No order status mapping for shipment status: {}", shipmentStatus);
            }
        } catch (Exception e) {
            log.error("Error processing shipment event", e);
        }
    }

    private OrderStatus mapShipmentStatusToOrderStatus(ShipmentDTO.ShipmentStatus shipmentStatus) {
        return switch (shipmentStatus) {
            case CREATED -> OrderStatus.PROCESSING;
            case IN_DELIVERY -> OrderStatus.SHIPPED;
            case DELIVERED -> OrderStatus.DELIVERED;
            case REJECTED -> OrderStatus.CANCELLED;
        };
    }
}
