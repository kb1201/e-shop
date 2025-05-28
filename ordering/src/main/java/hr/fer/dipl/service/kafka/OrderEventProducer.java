package hr.fer.dipl.service.kafka;

import hr.fer.dipl.dto.OrderDTO;
import hr.fer.dipl.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class OrderEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<Long, OrderEvent> kafkaTemplate;
    private final String orderEventTopic;

    public OrderEventProducer(
            @Qualifier("shipmentKafkaTemplate") KafkaTemplate<Long, OrderEvent> kafkaTemplate,
            @Value("${custom.kafka.shipment.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderEventTopic = topic;
    }

    public void sendOrderEvent(OrderDTO order) {
        try {
            OrderEvent event = new OrderEvent(
                    order.getId(),
                    order.getShippingAddress(),
                    order.getBillingAddress(),
                    order.getCreatedAt(),
                    order.getUpdatedAt(),
                    order.getStatus()
            );

            CompletableFuture<SendResult<Long, OrderEvent>> future =
                    kafkaTemplate.send(orderEventTopic, order.getId(), event);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    logger.error("Async callback - Failed to send event for orderId={}", order.getId(), exception);
                } else {
                    logger.debug("Async callback - Event sent: topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

            // Block and wait for completion - will throw exception if failed
            future.get();
            logger.info("Event sent successfully for orderId={}", order.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted while sending order event for orderId={}", order.getId(), e);
            throw new RuntimeException("Thread interrupted while sending order event", e);
        } catch (ExecutionException e) {
            logger.error("Failed to send order event for orderId={}", order.getId(), e.getCause());
            throw new RuntimeException("Failed to send order event", e.getCause());
        } catch (Exception e) {
            logger.error("Unexpected error sending order event for orderId={}", order.getId(), e);
            throw new RuntimeException("Failed to send order event", e);
        }
    }

}