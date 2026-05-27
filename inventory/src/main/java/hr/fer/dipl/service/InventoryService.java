package hr.fer.dipl.service;

import hr.fer.dipl.db.model.InventoryStatus;
import hr.fer.dipl.dto.InventoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {

    InventoryDTO getInventoryByProductId(Long productId);

    InventoryDTO getInventoryBySku(String sku);

    Page<InventoryDTO> getInventory(Pageable pageable);

    Page<InventoryDTO> getInventoryByStatus(InventoryStatus status, Pageable pageable);

    List<InventoryDTO> getItemsNeedingRestock();

    InventoryDTO createInventory(InventoryDTO inventoryDTO);

    InventoryDTO updateInventory(Long id, InventoryDTO inventoryDTO);

    boolean checkAvailability(Long productId, int quantity);

    void reserveInventory(Long productId, int quantity);

    void releaseReservation(Long productId, int quantity);

    void commitReservation(Long productId, int quantity);

    InventoryDTO restockInventory(Long productId, int quantity);

    void deleteInventory(Long id);
}
