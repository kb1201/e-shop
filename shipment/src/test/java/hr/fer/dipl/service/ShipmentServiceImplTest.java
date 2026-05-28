package hr.fer.dipl.service;

import hr.fer.dipl.db.model.Shipment;
import hr.fer.dipl.db.model.ShipmentStatus;
import hr.fer.dipl.db.repository.ShipmentRepository;
import hr.fer.dipl.dto.OrderEvent;
import hr.fer.dipl.dto.ShipmentDTO;
import hr.fer.dipl.dto.ShipmentStatusUpdateDTO;
import hr.fer.dipl.mapper.ShipmentMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ShipmentMapper shipmentMapper;
    @Mock private KafkaProducerService producerService;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private Shipment sampleShipment;
    private ShipmentDTO sampleShipmentDTO;
    private OrderEvent sampleOrderEvent;

    @BeforeEach
    void setUp() {
        sampleShipment = new Shipment(
                1L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "123 Main St",
                "123 Main St",
                ShipmentStatus.CREATED,
                10L
        );

        sampleOrderEvent = new OrderEvent(10L, "123 Main St", "123 Main St",
                LocalDateTime.now(), LocalDateTime.now());

        sampleShipmentDTO = new ShipmentDTO(1L, LocalDateTime.now(), LocalDateTime.now(),
                ShipmentStatus.CREATED, sampleOrderEvent);
    }

    // ------------------------------------------------------------------ getAllShipments

    @Test
    void getAllShipments_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shipment> page = new PageImpl<>(List.of(sampleShipment), pageable, 1);

        when(shipmentRepository.findAll(pageable)).thenReturn(page);
        when(shipmentMapper.toDTO(sampleShipment)).thenReturn(sampleShipmentDTO);

        Page<ShipmentDTO> result = shipmentService.getAllShipments(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ShipmentStatus.CREATED);
    }

    @Test
    void getAllShipments_returnsEmptyPage_whenNoShipments() {
        Pageable pageable = PageRequest.of(0, 10);
        when(shipmentRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        Page<ShipmentDTO> result = shipmentService.getAllShipments(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ------------------------------------------------------------------ getShipment

    @Test
    void getShipment_returnsDTO_whenFound() {
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(sampleShipment));
        when(shipmentMapper.toDTO(sampleShipment)).thenReturn(sampleShipmentDTO);

        ShipmentDTO result = shipmentService.getShipment(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.CREATED);
    }

    @Test
    void getShipment_throws_whenNotFound() {
        when(shipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.getShipment(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ------------------------------------------------------------------ createShipment

    @Test
    void createShipment_savesShipmentAndSendsKafkaEvent() {
        when(shipmentMapper.createFromOrderDTO(sampleOrderEvent)).thenReturn(sampleShipment);
        when(shipmentRepository.save(sampleShipment)).thenReturn(sampleShipment);
        when(shipmentMapper.toDTO(sampleShipment)).thenReturn(sampleShipmentDTO);
        doNothing().when(producerService).sendShipmentStatusUpdate(sampleShipment);

        ShipmentDTO result = shipmentService.createShipment(sampleOrderEvent);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(shipmentMapper).createFromOrderDTO(sampleOrderEvent);
        verify(shipmentRepository).save(sampleShipment);
        verify(producerService).sendShipmentStatusUpdate(sampleShipment);
    }

    @Test
    void createShipment_returnsDTO_withStatusFromMapper() {
        when(shipmentMapper.createFromOrderDTO(sampleOrderEvent)).thenReturn(sampleShipment);
        when(shipmentRepository.save(sampleShipment)).thenReturn(sampleShipment);
        when(shipmentMapper.toDTO(sampleShipment)).thenReturn(sampleShipmentDTO);
        doNothing().when(producerService).sendShipmentStatusUpdate(any());

        ShipmentDTO result = shipmentService.createShipment(sampleOrderEvent);

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.CREATED);
    }

    // ------------------------------------------------------------------ updateShipmentStatus â€” valid transitions

    @Test
    void updateShipmentStatus_createdToInDelivery_success() {
        sampleShipment.setStatus(ShipmentStatus.CREATED);
        ShipmentStatusUpdateDTO update = new ShipmentStatusUpdateDTO(ShipmentStatus.IN_DELIVERY);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(sampleShipment));
        when(shipmentRepository.save(sampleShipment)).thenReturn(sampleShipment);
        when(shipmentMapper.toDTO(sampleShipment)).thenReturn(sampleShipmentDTO);
        doNothing().when(producerService).sendShipmentStatusUpdate(sampleShipment);

        shipmentService.updateShipmentStatus(1L, update);

        assertThat(sampleShipment.getStatus()).isEqualTo(ShipmentStatus.IN_DELIVERY);
        verify(shipmentRepository).save(sampleShipment);
        verify(producerService).sendShipmentStatusUpdate(sampleShipment);
    }

    @Test
    void updateShipmentStatus_inDeliveryToDelivered_success() {
        sampleShipment.setStatus(ShipmentStatus.IN_DELIVERY);
        ShipmentStatusUpdateDTO update = new ShipmentStatusUpdateDTO(ShipmentStatus.DELIVERED);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(sampleShipment));
        when(shipmentRepository.save(sampleShipment)).thenReturn(sampleShipment);
        when(shipmentMapper.toDTO(sampleShipment)).thenReturn(sampleShipmentDTO);
        doNothing().when(producerService).sendShipmentStatusUpdate(any());

        shipmentService.updateShipmentStatus(1L, update);

        assertThat(sampleShipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @Test
    void updateShipmentStatus_createdToRejected_success() {
        sampleShipment.setStatus(ShipmentStatus.CREATED);
        ShipmentStatusUpdateDTO update = new ShipmentStatusUpdateDTO(ShipmentStatus.REJECTED);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(sampleShipment));
        when(shipmentRepository.save(sampleShipment)).thenReturn(sampleShipment);
        when(shipmentMapper.toDTO(sampleShipment)).thenReturn(sampleShipmentDTO);
        doNothing().when(producerService).sendShipmentStatusUpdate(any());

        shipmentService.updateShipmentStatus(1L, update);

        assertThat(sampleShipment.getStatus()).isEqualTo(ShipmentStatus.REJECTED);
    }

    // ------------------------------------------------------------------ updateShipmentStatus â€” terminal states

    @Test
    void updateShipmentStatus_throws_whenCurrentStatusIsDelivered() {
        sampleShipment.setStatus(ShipmentStatus.DELIVERED);
        ShipmentStatusUpdateDTO update = new ShipmentStatusUpdateDTO(ShipmentStatus.IN_DELIVERY);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(sampleShipment));

        assertThatThrownBy(() -> shipmentService.updateShipmentStatus(1L, update))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DELIVERED")
                .hasMessageContaining("final");

        verify(shipmentRepository, never()).save(any());
        verify(producerService, never()).sendShipmentStatusUpdate(any());
    }

    @Test
    void updateShipmentStatus_throws_whenCurrentStatusIsRejected() {
        sampleShipment.setStatus(ShipmentStatus.REJECTED);
        ShipmentStatusUpdateDTO update = new ShipmentStatusUpdateDTO(ShipmentStatus.CREATED);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(sampleShipment));

        assertThatThrownBy(() -> shipmentService.updateShipmentStatus(1L, update))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REJECTED")
                .hasMessageContaining("final");

        verify(shipmentRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ updateShipmentStatus â€” invalid transitions

    @Test
    void updateShipmentStatus_throws_whenGoingBackFromInDeliveryToCreated() {
        sampleShipment.setStatus(ShipmentStatus.IN_DELIVERY);
        ShipmentStatusUpdateDTO update = new ShipmentStatusUpdateDTO(ShipmentStatus.CREATED);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(sampleShipment));

        assertThatThrownBy(() -> shipmentService.updateShipmentStatus(1L, update))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IN_DELIVERY")
                .hasMessageContaining("CREATED");

        verify(shipmentRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ updateShipmentStatus â€” not found

    @Test
    void updateShipmentStatus_throws_whenShipmentNotFound() {
        when(shipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.updateShipmentStatus(99L,
                new ShipmentStatusUpdateDTO(ShipmentStatus.IN_DELIVERY)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ------------------------------------------------------------------ updateShipmentStatus â€” updatedAt is refreshed

    @Test
    void updateShipmentStatus_setsUpdatedAt_beforeSaving() {
        sampleShipment.setStatus(ShipmentStatus.CREATED);
        sampleShipment.setUpdatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        ShipmentStatusUpdateDTO update = new ShipmentStatusUpdateDTO(ShipmentStatus.IN_DELIVERY);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(sampleShipment));
        when(shipmentRepository.save(sampleShipment)).thenReturn(sampleShipment);
        when(shipmentMapper.toDTO(any())).thenReturn(sampleShipmentDTO);
        doNothing().when(producerService).sendShipmentStatusUpdate(any());

        shipmentService.updateShipmentStatus(1L, update);

        assertThat(sampleShipment.getUpdatedAt()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
    }
}
