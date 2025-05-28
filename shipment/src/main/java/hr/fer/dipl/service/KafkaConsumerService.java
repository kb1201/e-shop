package hr.fer.dipl.service;

import hr.fer.dipl.dto.OrderEvent;
import hr.fer.dipl.dto.ShipmentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ShipmentService shipmentService;

    @KafkaListener(
            topics = "${spring.kafka.consumer.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderMessage(OrderEvent orderEvent) {
        log.info("Received order message: {}", orderEvent);
        try {
            ShipmentDTO shipmentDTO = shipmentService.createShipment(orderEvent);
            log.info("Shipment created: {}", shipmentDTO);
        } catch (Exception e) {
            log.error("Error processing order message", e);
        }
    }
}
