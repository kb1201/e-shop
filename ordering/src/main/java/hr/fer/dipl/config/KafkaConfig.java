package hr.fer.dipl.config;

import hr.fer.dipl.dto.OrderEvent;
import hr.fer.dipl.dto.ShipmentDTO;
import hr.fer.dipl.dto.UserProductEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${custom.kafka.shipment.acks}")
    private String shipmentAcks;

    @Value("${custom.kafka.shipment.retries}")
    private int shipmentRetries;

    @Value("${custom.kafka.shipment.linger-ms}")
    private int shipmentLinger;

    @Value("${custom.kafka.shipment.idempotence}")
    private boolean shipmentIdempotence;

    @Value("${custom.kafka.recommendation.acks}")
    private String recAcks;

    @Value("${custom.kafka.recommendation.retries}")
    private int recRetries;

    @Value("${custom.kafka.recommendation.linger-ms}")
    private int recLinger;

    @Value("${custom.kafka.recommendation.idempotence}")
    private boolean recIdempotence;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ProducerFactory<Long, OrderEvent> reliableProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, shipmentAcks);
        props.put(ProducerConfig.RETRIES_CONFIG, shipmentRetries);
        props.put(ProducerConfig.LINGER_MS_CONFIG, shipmentLinger);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, shipmentIdempotence);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<Long, OrderEvent> shipmentKafkaTemplate() {
        return new KafkaTemplate<>(reliableProducerFactory());
    }

    @Bean
    public ProducerFactory<Long, UserProductEvent> fastProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, recAcks);
        props.put(ProducerConfig.RETRIES_CONFIG, recRetries);
        props.put(ProducerConfig.LINGER_MS_CONFIG, recLinger);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, recIdempotence);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<Long, UserProductEvent> recommendationKafkaTemplate() {
        return new KafkaTemplate<>(fastProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, ShipmentDTO> shipmentConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(ShipmentDTO.class, false)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ShipmentDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ShipmentDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(shipmentConsumerFactory());
        return factory;
    }
}
