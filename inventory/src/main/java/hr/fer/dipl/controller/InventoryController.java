package hr.fer.dipl.controller;

import hr.fer.dipl.db.model.InventoryStatus;
import hr.fer.dipl.dto.InventoryDTO;
import hr.fer.dipl.dto.InventoryRequest;
import hr.fer.dipl.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // --- Inventory CRUD ---
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<InventoryDTO>> getAllInventory(
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(required = false) InventoryStatus status) {
        var inventoryPage = status == null
                ? inventoryService.getInventory(pageable)
                : inventoryService.getInventoryByStatus(status, pageable);
        return ResponseEntity.ok(inventoryPage);
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryDTO> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryDTO> getInventoryBySku(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getInventoryBySku(sku));
    }

//    @GetMapping("/status/{status}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<List<InventoryDTO>> getInventoryByStatus(@PathVariable InventoryStatus status) {
//        return ResponseEntity.ok(inventoryService.getInventoryByStatus(status));
//    }


    //TODO
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
    public ResponseEntity<InventoryDTO> restockInventory(@PathVariable Long productId, @RequestBody InventoryRequest request) {
        var res = inventoryService.restockInventory(productId, request.getQuantity());
        return ResponseEntity.ok(res);
    }


}
