package hr.fer.dipl.service;

import hr.fer.dipl.dto.OrderEvent;
import hr.fer.dipl.dto.ShipmentDTO;
import hr.fer.dipl.dto.ShipmentStatusUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShipmentService {

    Page<ShipmentDTO> getAllShipments(Pageable pageable);

    ShipmentDTO getShipment(Long id);

    ShipmentDTO createShipment(OrderEvent orderDTO);

    ShipmentDTO updateShipmentStatus(Long id, ShipmentStatusUpdateDTO statusUpdateDTO);
}
