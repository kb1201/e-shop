package hr.fer.dipl.mapper;


import hr.fer.dipl.db.model.Shipment;
import hr.fer.dipl.db.model.ShipmentStatus;
import hr.fer.dipl.dto.OrderEvent;
import hr.fer.dipl.dto.ShipmentDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ShipmentMapper {

    public ShipmentDTO toDTO(Shipment shipment) {
        if (shipment == null) {
            return null;
        }

        OrderEvent orderDTO = new OrderEvent();
        orderDTO.setOrderId(shipment.getOrderId());
        orderDTO.setShippingAddress(shipment.getShippingAddress());
        orderDTO.setBillingAddress(shipment.getBillingAddress());

        return new ShipmentDTO(
                shipment.getId(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt(),
                shipment.getStatus(),
                orderDTO
        );
    }

    public Shipment toEntity(ShipmentDTO shipmentDTO) {
        if (shipmentDTO == null) {
            return null;
        }

        Shipment shipment = new Shipment();
        shipment.setId(shipmentDTO.getId());
        shipment.setCreatedAt(shipmentDTO.getCreatedAt());
        shipment.setUpdatedAt(shipmentDTO.getUpdatedAt());
        shipment.setStatus(shipmentDTO.getStatus());
        shipment.setBillingAddress(shipmentDTO.getOrder().getBillingAddress());
        shipment.setShippingAddress(shipmentDTO.getOrder().getShippingAddress());

        if (shipmentDTO.getOrder() != null) {
            shipment.setOrderId(shipmentDTO.getOrder().getOrderId());
        }

        return shipment;
    }

    public Shipment createFromOrderDTO(OrderEvent orderDTO) {
        Shipment shipment = new Shipment();
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setOrderId(orderDTO.getOrderId());
        shipment.setBillingAddress(orderDTO.getBillingAddress());
        shipment.setShippingAddress(orderDTO.getShippingAddress());
        return shipment;
    }
}
