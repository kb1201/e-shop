package hr.fer.dipl.mapper;

import hr.fer.dipl.db.model.Shipment;
import hr.fer.dipl.db.model.ShipmentStatus;
import hr.fer.dipl.dto.OrderEvent;
import hr.fer.dipl.dto.ShipmentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ShipmentMapperTest {

    private ShipmentMapper shipmentMapper;

    @BeforeEach
    void setUp() {
        shipmentMapper = new ShipmentMapper();
    }

    // ------------------------------------------------------------------ toDTO

    @Test
    void toDTO_mapsAllFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        Shipment shipment = new Shipment(1L, now, now, "123 Main St", "456 Bill Ave",
                ShipmentStatus.IN_DELIVERY, 10L);

        ShipmentDTO result = shipmentMapper.toDTO(shipment);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.IN_DELIVERY);
        assertThat(result.getOrder()).isNotNull();
        assertThat(result.getOrder().getOrderId()).isEqualTo(10L);
        assertThat(result.getOrder().getShippingAddress()).isEqualTo("123 Main St");
        assertThat(result.getOrder().getBillingAddress()).isEqualTo("456 Bill Ave");
    }

    @Test
    void toDTO_returnsNull_whenShipmentIsNull() {
        assertThat(shipmentMapper.toDTO(null)).isNull();
    }

    // ------------------------------------------------------------------ toEntity

    @Test
    void toEntity_mapsAllFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        OrderEvent orderEvent = new OrderEvent(10L, "123 Main St", "456 Bill Ave", now, now);
        ShipmentDTO dto = new ShipmentDTO(1L, now, now, ShipmentStatus.CREATED, orderEvent);

        Shipment result = shipmentMapper.toEntity(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(result.getShippingAddress()).isEqualTo("123 Main St");
        assertThat(result.getBillingAddress()).isEqualTo("456 Bill Ave");
        assertThat(result.getOrderId()).isEqualTo(10L);
    }

    @Test
    void toEntity_returnsNull_whenDtoIsNull() {
        assertThat(shipmentMapper.toEntity(null)).isNull();
    }

    // ------------------------------------------------------------------ createFromOrderDTO

    @Test
    void createFromOrderDTO_setsCreatedStatusAndCopiesFields() {
        OrderEvent orderEvent = new OrderEvent(20L, "Ship St", "Bill Ave", null, null);

        Shipment result = shipmentMapper.createFromOrderDTO(orderEvent);

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(result.getOrderId()).isEqualTo(20L);
        assertThat(result.getShippingAddress()).isEqualTo("Ship St");
        assertThat(result.getBillingAddress()).isEqualTo("Bill Ave");
    }

    @Test
    void createFromOrderDTO_setsCreatedAtTimestamp() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        OrderEvent orderEvent = new OrderEvent(1L, "A", "B", null, null);

        Shipment result = shipmentMapper.createFromOrderDTO(orderEvent);

        assertThat(result.getCreatedAt()).isAfter(before);
    }

    @Test
    void createFromOrderDTO_doesNotSetId() {
        OrderEvent orderEvent = new OrderEvent(1L, "A", "B", null, null);

        Shipment result = shipmentMapper.createFromOrderDTO(orderEvent);

        assertThat(result.getId()).isNull();
    }
}
