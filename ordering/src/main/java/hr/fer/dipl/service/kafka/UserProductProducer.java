package hr.fer.dipl.service.kafka;

import hr.fer.dipl.dto.OrderDTO;
import hr.fer.dipl.dto.UserProductEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class UserProductProducer {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<Long, UserProductEvent> kafkaTemplate;
    private final String userProductTopic;

    public UserProductProducer(
            @Qualifier("recommendationKafkaTemplate") KafkaTemplate<Long, UserProductEvent> kafkaTemplate,
            @Value("${custom.kafka.recommendation.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.userProductTopic = topic;
    }


    public void sendUserProductEvent(OrderDTO order) {
        order.getItems().forEach(item -> {
            UserProductEvent event = new UserProductEvent(order.getUserId(), item.getProductId(), item.getQuantity(), System.currentTimeMillis());

            try {

                CompletableFuture<SendResult<Long, UserProductEvent>> future = kafkaTemplate.send(userProductTopic, order.getUserId(), event);

                future.whenComplete((result, exception) -> {
                    if (exception != null) {
                        logger.error("Failed to send event", exception);
                    } else {
                        logger.debug("Event sent: topic={}, partition={}, offset={}", result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });

                logger.info("Event sent for userId={}, productId={}", order.getUserId(), item.getProductId());

            } catch (Exception e) {
                logger.error("Error serializing or sending user-product event", e);
            }
        });
    }


}
