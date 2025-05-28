package hr.fer.dipl.controller;


import hr.fer.dipl.dto.ShipmentDTO;
import hr.fer.dipl.dto.ShipmentStatusUpdateDTO;
import hr.fer.dipl.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    
    private final ShipmentService shipmentService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ShipmentDTO> getAllShipments() {
        return shipmentService.getAllShipments();
    }
    
    @GetMapping("/{id}")
    public ShipmentDTO getShipment(@PathVariable Long id) {
        return shipmentService.getShipment(id);
    }
    
    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public ShipmentDTO updateShipmentStatus(@PathVariable Long id, @RequestBody ShipmentStatusUpdateDTO statusUpdateDTO) {
        return shipmentService.updateShipmentStatus(id, statusUpdateDTO);
    }
}