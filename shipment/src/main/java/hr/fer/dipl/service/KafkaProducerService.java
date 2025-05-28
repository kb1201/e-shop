package hr.fer.dipl.service;


import hr.fer.dipl.db.model.Shipment;
import hr.fer.dipl.dto.ShipmentDTO;
import hr.fer.dipl.mapper.ShipmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, ShipmentDTO> kafkaTemplate;
    private final ShipmentMapper shipmentMapper;
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    @Value("${spring.kafka.producer.topic}")
    private String topic;

    public void sendShipmentStatusUpdate(Shipment shipment) {
        ShipmentDTO dto = shipmentMapper.toDTO(shipment);
        kafkaTemplate.send(topic, dto).whenComplete((result, exception) -> {
            if (exception != null) {
                logger.error("Failed to send event", exception);
            } else {
                //todo exception handling ?? some manual task??
                logger.debug("Event sent: topic={}, partition={}, offset={}", result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
        log.info("Sent shipment status update: {}", dto);
    }
}