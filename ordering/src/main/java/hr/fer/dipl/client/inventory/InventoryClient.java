package hr.fer.dipl.client.inventory;


import hr.fer.dipl.client.inventory.model.InventoryDTO;
import hr.fer.dipl.client.inventory.model.InventoryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "inventory-service", url = "${inventory-service.url:http://localhost:8083}")
public interface InventoryClient {

    @GetMapping("/inventory/product/{productId}")
    InventoryDTO getInventoryByProductId(@PathVariable("productId") Long productId);

    @GetMapping("/inventory/{productId}/availability")
    Boolean checkAvailability(
            @PathVariable("productId") Long productId,
            @RequestParam("quantity") int quantity);

    @PostMapping("/inventory/{productId}/reservations")
    void reserveInventory(
            @PathVariable("productId") Long productId,
            @RequestBody InventoryRequest request);

    @DeleteMapping("/inventory/{productId}/reservations")
    void releaseReservation(
            @PathVariable("productId") Long productId,
            @RequestBody InventoryRequest request);

    @PostMapping("/inventory/{productId}/commit")
    void commitReservation(
            @PathVariable("productId") Long productId,
            @RequestBody InventoryRequest request);

    @PutMapping("/inventory/{productId}/restock")
    void restockInventory(
            @PathVariable("productId") Long productId,
            @RequestBody InventoryRequest request);

    @GetMapping("/inventory/restock-needed")
    List<InventoryDTO> getItemsNeedingRestock();
}