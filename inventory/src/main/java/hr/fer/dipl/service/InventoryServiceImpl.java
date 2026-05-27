package hr.fer.dipl.service;


import hr.fer.dipl.client.catalog.ProductCatalogClient;
import hr.fer.dipl.client.catalog.model.ProductNameDTO;
import hr.fer.dipl.db.model.Inventory;
import hr.fer.dipl.db.model.InventoryStatus;
import hr.fer.dipl.db.repository.InventoryRepository;
import hr.fer.dipl.dto.InventoryDTO;
import hr.fer.dipl.exception.InventoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductCatalogClient productCatalogClient;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository, ProductCatalogClient productCatalogClient) {
        this.inventoryRepository = inventoryRepository;
        this.productCatalogClient = productCatalogClient;
    }


    @Override
    public InventoryDTO getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));
        return convertToDTO(inventory);
    }

    @Override
    public InventoryDTO getInventoryBySku(String sku) {
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new InventoryException("Inventory not found for SKU: " + sku));
        return convertToDTO(inventory);
    }

    @Override
    public Page<InventoryDTO> getInventory(Pageable pageable) {
        Page<Inventory> inventoryPage = inventoryRepository.findAll(pageable);

        List<Long> productIds = inventoryPage
                .stream()
                .map(Inventory::getProductId)
                .distinct()
                .collect(Collectors.toList());

        // Fetch product names from catalog service
        try {
            List<ProductNameDTO> productNames = productCatalogClient.getProductNames(productIds);
            Map<Long, String> productNameMap = productNames
                    .stream()
                    .collect(Collectors.toMap(ProductNameDTO::getId, ProductNameDTO::getName));
            return inventoryPage.map(inventory -> {
                InventoryDTO dto = convertToDTO(inventory);
                dto.setProductName(productNameMap.get(dto.getProductId()));
                return dto;
            });

        } catch (Exception exc) {
            return inventoryPage.map(inventory -> {
                InventoryDTO dto = convertToDTO(inventory);
                dto.setProductName("Unknown product");
                return dto;
            });
        }
    }

    @Override
    public Page<InventoryDTO> getInventoryByStatus(InventoryStatus status, Pageable pageable) {
        return inventoryRepository.findByStatus(status, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public List<InventoryDTO> getItemsNeedingRestock() {
        return inventoryRepository.findItemsNeedingRestock().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
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

    @Override
    @Transactional
    public InventoryDTO updateInventory(Long id, InventoryDTO inventoryDTO) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryException("Inventory not found with ID: " + id));

// Update only non-null fields
        if (inventoryDTO.getQuantityAvailable() != null) {
            inventory.setQuantityAvailable(inventoryDTO.getQuantityAvailable());
        }
        if (inventoryDTO.getReorderThreshold() != null) {
            inventory.setReorderThreshold(inventoryDTO.getReorderThreshold());
        }
        if (inventoryDTO.getWarehouseLocation() != null) {
            inventory.setWarehouseLocation(inventoryDTO.getWarehouseLocation());
        }
        if (inventoryDTO.getShelfLocation() != null) {
            inventory.setShelfLocation(inventoryDTO.getShelfLocation());
        }

        inventory.setLastUpdated(LocalDateTime.now());
        inventory.updateStatus();

        Inventory updatedInventory = inventoryRepository.save(inventory);
        return convertToDTO(updatedInventory);

    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAvailability(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));

        return inventory.canReserve(quantity);
    }


    @Override
    @Transactional
    public void reserveInventory(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));

        if (!inventory.canReserve(quantity)) {
            throw new InventoryException("Not enough inventory available for product ID: " + productId);
        }

        inventory.reserve(quantity);
        inventory.updateStatus();
        inventoryRepository.save(inventory);
    }


    @Override
    @Transactional
    public void releaseReservation(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));

        inventory.releaseReservation(quantity);
        inventory.updateStatus();
        inventoryRepository.save(inventory);
    }


    @Override
    @Transactional
    public void commitReservation(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));

        inventory.commitReservation(quantity);
        inventory.updateStatus();
        inventoryRepository.save(inventory);
    }


    @Override
    @Transactional
    public InventoryDTO restockInventory(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new InventoryException("Inventory not found for product ID: " + productId));

        inventory.restock(quantity);
        return convertToDTO(inventoryRepository.save(inventory));
    }

    @Override
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