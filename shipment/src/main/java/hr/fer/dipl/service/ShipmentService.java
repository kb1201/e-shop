package hr.fer.dipl.service;


import hr.fer.dipl.db.model.Shipment;
import hr.fer.dipl.db.repository.ShipmentRepository;
import hr.fer.dipl.dto.OrderEvent;
import hr.fer.dipl.dto.ShipmentDTO;
import hr.fer.dipl.dto.ShipmentStatusUpdateDTO;
import hr.fer.dipl.mapper.ShipmentMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
    
    public List<ShipmentDTO> getAllShipments() {
        return shipmentRepository.findAll().stream()
                .map(shipmentMapper::toDTO)
                .collect(Collectors.toList());
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
        
        shipment.setStatus(statusUpdateDTO.getStatus());
        shipment.setUpdatedAt(LocalDateTime.now());
        
        Shipment updatedShipment = shipmentRepository.save(shipment);
        
        // Send notification about status change
        producerService.sendShipmentStatusUpdate(updatedShipment);
        
        return shipmentMapper.toDTO(updatedShipment);
    }
}