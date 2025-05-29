package hr.fer.dipl.service;


import hr.fer.dipl.db.model.Shipment;
import hr.fer.dipl.db.model.ShipmentStatus;
import hr.fer.dipl.db.repository.ShipmentRepository;
import hr.fer.dipl.dto.OrderEvent;
import hr.fer.dipl.dto.ShipmentDTO;
import hr.fer.dipl.dto.ShipmentStatusUpdateDTO;
import hr.fer.dipl.mapper.ShipmentMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final KafkaProducerService producerService;

    public Page<ShipmentDTO> getAllShipments(Pageable pageable) {
        return shipmentRepository.findAll(pageable)
                .map(shipmentMapper::toDTO);
    }

    public ShipmentDTO getShipment(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipment not found with ID: " + id));
        return shipmentMapper.toDTO(shipment);
    }

    @Transactional
    public ShipmentDTO createShipment(OrderEvent orderDTO) {
        Shipment shipment = shipmentMapper.createFromOrderDTO(orderDTO);
        Shipment savedShipment = shipmentRepository.save(shipment);

        // Send notification about new shipment
        producerService.sendShipmentStatusUpdate(savedShipment);

        return shipmentMapper.toDTO(savedShipment);
    }

    @Transactional
    public ShipmentDTO updateShipmentStatus(Long id, ShipmentStatusUpdateDTO statusUpdateDTO) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipment not found with ID: " + id));

        validateStatusTransition(shipment.getStatus(), statusUpdateDTO.getStatus());

        shipment.setStatus(statusUpdateDTO.getStatus());
        shipment.setUpdatedAt(LocalDateTime.now());

        Shipment updatedShipment = shipmentRepository.save(shipment);

        // Send notification about status change
        producerService.sendShipmentStatusUpdate(updatedShipment);

        return shipmentMapper.toDTO(updatedShipment);
    }

    private void validateStatusTransition(ShipmentStatus currentStatus, ShipmentStatus newStatus) {
        // Check if current status is final (cannot be changed)
        if (currentStatus == ShipmentStatus.DELIVERED || currentStatus == ShipmentStatus.REJECTED) {
            throw new IllegalStateException(
                    String.format("Cannot change status from %s to %s. Shipment with status %s is final and cannot be modified.",
                            currentStatus, newStatus, currentStatus)
            );
        }

        validateBusinessRules(currentStatus, newStatus);
    }

    private void validateBusinessRules(ShipmentStatus currentStatus, ShipmentStatus newStatus) {


        if (currentStatus == ShipmentStatus.IN_DELIVERY && newStatus == ShipmentStatus.CREATED) {
            throw new IllegalStateException("Cannot change status from IN_DELIVERY back to CREATED");
        }

    }
}