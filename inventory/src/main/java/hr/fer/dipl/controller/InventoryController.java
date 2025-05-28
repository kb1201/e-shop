package hr.fer.dipl.controller;

import hr.fer.dipl.db.model.InventoryStatus;
import hr.fer.dipl.dto.InventoryDTO;
import hr.fer.dipl.dto.InventoryRequest;
import hr.fer.dipl.service.InventoryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryServiceImpl inventoryService;

    @Autowired
    public InventoryController(InventoryServiceImpl inventoryService) {
        this.inventoryService = inventoryService;
    }

    // --- Inventory CRUD ---
    @GetMapping
    public ResponseEntity<List<InventoryDTO>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryDTO> createInventory(@RequestBody InventoryDTO inventoryDTO) {
        return new ResponseEntity<>(inventoryService.createInventory(inventoryDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryDTO> updateInventory(@PathVariable Long id, @RequestBody InventoryDTO inventoryDTO) {
        return ResponseEntity.ok(inventoryService.updateInventory(id, inventoryDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }

    // --- Inventory Queries ---
    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryDTO> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<InventoryDTO> getInventoryBySku(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getInventoryBySku(sku));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InventoryDTO>> getInventoryByStatus(@PathVariable InventoryStatus status) {
        return ResponseEntity.ok(inventoryService.getInventoryByStatus(status));
    }


    @GetMapping("/restock-needed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryDTO>> getItemsNeedingRestock() {
        return ResponseEntity.ok(inventoryService.getItemsNeedingRestock());
    }

    // --- Availability Check ---
    @GetMapping("/{productId}/availability")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable Long productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(inventoryService.checkAvailability(productId, quantity));
    }

    // --- Reservation Sub-resource ---
    @PostMapping("/{productId}/reservations")
    public ResponseEntity<Void> reserveInventory(@PathVariable Long productId, @RequestBody InventoryRequest request) {
        inventoryService.reserveInventory(productId, request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}/reservations")
    public ResponseEntity<Void> releaseReservation(@PathVariable Long productId, @RequestBody InventoryRequest request) {
        inventoryService.releaseReservation(productId, request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{productId}/commit")
    public ResponseEntity<Void> commitReservation(@PathVariable Long productId, @RequestBody InventoryRequest request) {
        inventoryService.commitReservation(productId, request.getQuantity());
        return ResponseEntity.ok().build();
    }

    // --- Restock ---
    @PutMapping("/{productId}/restock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restockInventory(@PathVariable Long productId, @RequestBody InventoryRequest request) {
        inventoryService.restockInventory(productId, request.getQuantity());
        return ResponseEntity.ok().build();
    }


}
