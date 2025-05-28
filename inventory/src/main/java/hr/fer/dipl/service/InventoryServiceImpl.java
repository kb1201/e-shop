package hr.fer.dipl.service;


import hr.fer.dipl.db.model.Inventory;
import hr.fer.dipl.db.model.InventoryStatus;
import hr.fer.dipl.db.repository.InventoryRepository;
import hr.fer.dipl.dto.InventoryDTO;
import hr.fer.dipl.exception.InventoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl   {

    private final InventoryRepository inventoryRepository;
    
    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }
    

    public InventoryDTO getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));
        return convertToDTO(inventory);
    }
    
    public InventoryDTO getInventoryBySku(String sku) {
        Inventory inventory = inventoryRepository.findBySku(sku)
            .orElseThrow(() -> new InventoryException("Inventory not found for SKU: " + sku));
        return convertToDTO(inventory);
    }
    
    public List<InventoryDTO> getAllInventory() {
        return inventoryRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<InventoryDTO> getInventoryByStatus(InventoryStatus status) {
        return inventoryRepository.findByStatus(status).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<InventoryDTO> getItemsNeedingRestock() {
        return inventoryRepository.findItemsNeedingRestock().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public InventoryDTO createInventory(InventoryDTO inventoryDTO) {
        // Check if inventory already exists for this product
        if (inventoryRepository.findByProductId(inventoryDTO.getProductId()).isPresent()) {
            throw new InventoryException("Inventory already exists for product ID: " + inventoryDTO.getProductId());
        }
        
        if (inventoryRepository.findBySku(inventoryDTO.getSku()).isPresent()) {
            throw new InventoryException("Inventory already exists for SKU: " + inventoryDTO.getSku());
        }
        
        Inventory inventory = convertToEntity(inventoryDTO);
        inventory.setLastUpdated(LocalDateTime.now());
        inventory.updateStatus();
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        return convertToDTO(savedInventory);
    }
    
    @Transactional
    public InventoryDTO updateInventory(Long id, InventoryDTO inventoryDTO) {
        Inventory inventory = inventoryRepository.findById(id)
            .orElseThrow(() -> new InventoryException("Inventory not found with ID: " + id));
        
        // Update fields
        inventory.setQuantityAvailable(inventoryDTO.getQuantityAvailable());
        inventory.setReorderThreshold(inventoryDTO.getReorderThreshold());
        inventory.setWarehouseLocation(inventoryDTO.getWarehouseLocation());
        inventory.setShelfLocation(inventoryDTO.getShelfLocation());
        inventory.setLastUpdated(LocalDateTime.now());
        inventory.updateStatus();
        
        Inventory updatedInventory = inventoryRepository.save(inventory);
        return convertToDTO(updatedInventory);
    }
    
    @Transactional(readOnly = true)
    public boolean checkAvailability(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));
        
        return inventory.canReserve(quantity);
    }
    

    @Transactional
    public void reserveInventory(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
            .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));
        
        if (!inventory.canReserve(quantity)) {
            throw new InventoryException("Not enough inventory available for product ID: " + productId);
        }
        
        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
    }
    

    @Transactional
    public void releaseReservation(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
            .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));
        
        inventory.releaseReservation(quantity);
        inventoryRepository.save(inventory);
    }
    

    @Transactional
    public void commitReservation(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
            .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));
        
        inventory.commitReservation(quantity);
        inventoryRepository.save(inventory);
    }
    

    @Transactional
    public void restockInventory(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
            .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));
        
        inventory.restock(quantity);
        inventoryRepository.save(inventory);
    }
    
    @Transactional
    public void deleteInventory(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new InventoryException("Inventory not found with ID: " + id);
        }
        inventoryRepository.deleteById(id);
    }
    
    // Helper methods for DTO conversion
    private InventoryDTO convertToDTO(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(inventory.getId());
        dto.setProductId(inventory.getProductId());
        dto.setSku(inventory.getSku());
        dto.setQuantityAvailable(inventory.getQuantityAvailable());
        dto.setReservedQuantity(inventory.getReservedQuantity());
        dto.setReorderThreshold(inventory.getReorderThreshold());
        dto.setWarehouseLocation(inventory.getWarehouseLocation());
        dto.setShelfLocation(inventory.getShelfLocation());
        dto.setStatus(inventory.getStatus());
        dto.setLastUpdated(inventory.getLastUpdated());
        return dto;
    }
    
    private Inventory convertToEntity(InventoryDTO dto) {
        Inventory inventory = new Inventory();
        inventory.setProductId(dto.getProductId());
        inventory.setSku(dto.getSku());
        inventory.setQuantityAvailable(dto.getQuantityAvailable());
        inventory.setReservedQuantity(dto.getReservedQuantity() != null ? dto.getReservedQuantity() : 0);
        inventory.setReorderThreshold(dto.getReorderThreshold());
        inventory.setWarehouseLocation(dto.getWarehouseLocation());
        inventory.setShelfLocation(dto.getShelfLocation());
        return inventory;
    }
}