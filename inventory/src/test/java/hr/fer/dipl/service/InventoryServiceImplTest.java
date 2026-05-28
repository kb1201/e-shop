package hr.fer.dipl.service;

import hr.fer.dipl.client.catalog.ProductCatalogClient;
import hr.fer.dipl.client.catalog.model.ProductNameDTO;
import hr.fer.dipl.db.model.Inventory;
import hr.fer.dipl.db.model.InventoryStatus;
import hr.fer.dipl.db.repository.InventoryRepository;
import hr.fer.dipl.dto.InventoryDTO;
import hr.fer.dipl.exception.InventoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory sampleInventory;

    @BeforeEach
    void setUp() {
        sampleInventory = Inventory.builder()
                .id(1L)
                .productId(10L)
                .sku("SKU-001")
                .quantityAvailable(100)
                .reservedQuantity(0)
                .reorderThreshold(10)
                .warehouseLocation("WH-A")
                .shelfLocation("S-01")
                .status(InventoryStatus.IN_STOCK)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    // ------------------------------------------------------------------ getInventoryByProductId

    @Test
    void getInventoryByProductId_returnsDTO_whenFound() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(sampleInventory));

        InventoryDTO result = inventoryService.getInventoryByProductId(10L);

        assertThat(result.getProductId()).isEqualTo(10L);
        assertThat(result.getSku()).isEqualTo("SKU-001");
        assertThat(result.getQuantityAvailable()).isEqualTo(100);
    }

    @Test
    void getInventoryByProductId_throws_whenNotFound() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryByProductId(99L))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("99");
    }

    // ------------------------------------------------------------------ getInventoryBySku

    @Test
    void getInventoryBySku_returnsDTO_whenFound() {
        when(inventoryRepository.findBySku("SKU-001")).thenReturn(Optional.of(sampleInventory));

        InventoryDTO result = inventoryService.getInventoryBySku("SKU-001");

        assertThat(result.getSku()).isEqualTo("SKU-001");
        assertThat(result.getProductId()).isEqualTo(10L);
    }

    @Test
    void getInventoryBySku_throws_whenNotFound() {
        when(inventoryRepository.findBySku("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryBySku("MISSING"))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("MISSING");
    }

    // ------------------------------------------------------------------ getInventory (paginated)

    @Test
    void getInventory_enrichesWithProductNames_whenCatalogSucceeds() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> page = new PageImpl<>(List.of(sampleInventory), pageable, 1);

        when(inventoryRepository.findAll(pageable)).thenReturn(page);
        when(productCatalogClient.getProductNames(List.of(10L)))
                .thenReturn(List.of(new ProductNameDTO(10L, "Widget")));

        Page<InventoryDTO> result = inventoryService.getInventory(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("Widget");
    }

    @Test
    void getInventory_setsUnknownProduct_whenCatalogThrows() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> page = new PageImpl<>(List.of(sampleInventory), pageable, 1);

        when(inventoryRepository.findAll(pageable)).thenReturn(page);
        when(productCatalogClient.getProductNames(anyList()))
                .thenThrow(new RuntimeException("catalog down"));

        Page<InventoryDTO> result = inventoryService.getInventory(pageable);

        assertThat(result.getContent().get(0).getProductName()).isEqualTo("Unknown product");
    }

    @Test
    void getInventory_returnsEmptyPage_whenNoInventory() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(inventoryRepository.findAll(pageable)).thenReturn(emptyPage);
        when(productCatalogClient.getProductNames(List.of())).thenReturn(List.of());

        Page<InventoryDTO> result = inventoryService.getInventory(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ------------------------------------------------------------------ getInventoryByStatus

    @Test
    void getInventoryByStatus_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> page = new PageImpl<>(List.of(sampleInventory), pageable, 1);

        when(inventoryRepository.findByStatus(InventoryStatus.IN_STOCK, pageable)).thenReturn(page);

        Page<InventoryDTO> result = inventoryService.getInventoryByStatus(InventoryStatus.IN_STOCK, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(InventoryStatus.IN_STOCK);
    }

    // ------------------------------------------------------------------ getItemsNeedingRestock

    @Test
    void getItemsNeedingRestock_returnsAllLowOrOutOfStock() {
        Inventory lowStock = Inventory.builder()
                .id(2L).productId(20L).sku("SKU-002")
                .quantityAvailable(5).reservedQuantity(0).reorderThreshold(10)
                .status(InventoryStatus.LOW_STOCK).lastUpdated(LocalDateTime.now()).build();

        when(inventoryRepository.findItemsNeedingRestock()).thenReturn(List.of(sampleInventory, lowStock));

        List<InventoryDTO> result = inventoryService.getItemsNeedingRestock();

        assertThat(result).hasSize(2);
    }

    // ------------------------------------------------------------------ createInventory

    @Test
    void createInventory_savesAndReturnsDTO() {
        InventoryDTO dto = buildDTO(null, 10L, "SKU-001", 100, 0, 10);
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.empty());
        when(inventoryRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(sampleInventory);

        InventoryDTO result = inventoryService.createInventory(dto);

        assertThat(result.getProductId()).isEqualTo(10L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void createInventory_throws_whenProductIdAlreadyExists() {
        InventoryDTO dto = buildDTO(null, 10L, "SKU-NEW", 50, 0, 5);
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(sampleInventory));

        assertThatThrownBy(() -> inventoryService.createInventory(dto))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("already exists")
                .hasMessageContaining("10");
    }

    @Test
    void createInventory_throws_whenSkuAlreadyExists() {
        InventoryDTO dto = buildDTO(null, 99L, "SKU-001", 50, 0, 5);
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());
        when(inventoryRepository.findBySku("SKU-001")).thenReturn(Optional.of(sampleInventory));

        assertThatThrownBy(() -> inventoryService.createInventory(dto))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("already exists")
                .hasMessageContaining("SKU-001");
    }

    // ------------------------------------------------------------------ updateInventory

    @Test
    void updateInventory_updatesAllProvidedFields() {
        InventoryDTO updateDTO = InventoryDTO.builder()
                .quantityAvailable(200)
                .reorderThreshold(20)
                .warehouseLocation("WH-B")
                .shelfLocation("S-99")
                .build();

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(sampleInventory)).thenReturn(sampleInventory);

        InventoryDTO result = inventoryService.updateInventory(1L, updateDTO);

        // Verify entity fields were mutated before save
        assertThat(sampleInventory.getQuantityAvailable()).isEqualTo(200);
        assertThat(sampleInventory.getReorderThreshold()).isEqualTo(20);
        assertThat(sampleInventory.getWarehouseLocation()).isEqualTo("WH-B");
        assertThat(sampleInventory.getShelfLocation()).isEqualTo("S-99");
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void updateInventory_doesNotOverwriteNullFields() {
        // Only quantityAvailable is provided; location fields should be untouched
        InventoryDTO updateDTO = InventoryDTO.builder()
                .quantityAvailable(50)
                .build();

        String originalWarehouse = sampleInventory.getWarehouseLocation();
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(sampleInventory)).thenReturn(sampleInventory);

        inventoryService.updateInventory(1L, updateDTO);

        assertThat(sampleInventory.getQuantityAvailable()).isEqualTo(50);
        assertThat(sampleInventory.getWarehouseLocation()).isEqualTo(originalWarehouse);
    }

    @Test
    void updateInventory_throws_whenInventoryNotFound() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.updateInventory(99L, new InventoryDTO()))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("99");
    }

    // ------------------------------------------------------------------ checkAvailability

    @Test
    void checkAvailability_returnsTrue_whenEnoughStock() {
        sampleInventory.setQuantityAvailable(100);
        sampleInventory.setReservedQuantity(0);
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(sampleInventory));

        assertThat(inventoryService.checkAvailability(10L, 50)).isTrue();
    }

    @Test
    void checkAvailability_returnsFalse_whenNotEnoughStock() {
        sampleInventory.setQuantityAvailable(10);
        sampleInventory.setReservedQuantity(8);  // net = 2
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(sampleInventory));

        assertThat(inventoryService.checkAvailability(10L, 3)).isFalse();
    }

    @Test
    void checkAvailability_throws_whenProductNotFound() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.checkAvailability(99L, 1))
                .isInstanceOf(InventoryException.class);
    }

    // ------------------------------------------------------------------ reserveInventory

    @Test
    void reserveInventory_savesUpdatedInventory() {
        sampleInventory.setQuantityAvailable(50);
        sampleInventory.setReservedQuantity(0);
        when(inventoryRepository.findByProductIdWithLock(10L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(sampleInventory)).thenReturn(sampleInventory);

        inventoryService.reserveInventory(10L, 10);

        assertThat(sampleInventory.getReservedQuantity()).isEqualTo(10);
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void reserveInventory_throws_whenNotFound() {
        when(inventoryRepository.findByProductIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserveInventory(99L, 5))
                .isInstanceOf(InventoryException.class);
    }

    @Test
    void reserveInventory_throws_whenNotEnoughAvailable() {
        sampleInventory.setQuantityAvailable(5);
        sampleInventory.setReservedQuantity(5);  // net = 0
        when(inventoryRepository.findByProductIdWithLock(10L)).thenReturn(Optional.of(sampleInventory));

        assertThatThrownBy(() -> inventoryService.reserveInventory(10L, 1))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("Not enough inventory");
    }

    // ------------------------------------------------------------------ releaseReservation

    @Test
    void releaseReservation_savesUpdatedInventory() {
        sampleInventory.setQuantityAvailable(50);
        sampleInventory.setReservedQuantity(20);
        when(inventoryRepository.findByProductIdWithLock(10L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(sampleInventory)).thenReturn(sampleInventory);

        inventoryService.releaseReservation(10L, 10);

        assertThat(sampleInventory.getReservedQuantity()).isEqualTo(10);
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void releaseReservation_throws_whenNotFound() {
        when(inventoryRepository.findByProductIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.releaseReservation(99L, 5))
                .isInstanceOf(InventoryException.class);
    }

    // ------------------------------------------------------------------ commitReservation

    @Test
    void commitReservation_decreasesBothQuantities() {
        sampleInventory.setQuantityAvailable(50);
        sampleInventory.setReservedQuantity(20);
        when(inventoryRepository.findByProductIdWithLock(10L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(sampleInventory)).thenReturn(sampleInventory);

        inventoryService.commitReservation(10L, 15);

        assertThat(sampleInventory.getReservedQuantity()).isEqualTo(5);
        assertThat(sampleInventory.getQuantityAvailable()).isEqualTo(35);
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void commitReservation_throws_whenNotFound() {
        when(inventoryRepository.findByProductIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.commitReservation(99L, 5))
                .isInstanceOf(InventoryException.class);
    }

    // ------------------------------------------------------------------ restockInventory

    @Test
    void restockInventory_increasesQuantityAndReturnsDTO() {
        sampleInventory.setQuantityAvailable(10);
        when(inventoryRepository.findByProductIdWithLock(10L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(sampleInventory)).thenReturn(sampleInventory);

        InventoryDTO result = inventoryService.restockInventory(10L, 50);

        assertThat(sampleInventory.getQuantityAvailable()).isEqualTo(60);
        assertThat(result).isNotNull();
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void restockInventory_throws_whenNotFound() {
        when(inventoryRepository.findByProductIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.restockInventory(99L, 10))
                .isInstanceOf(InventoryException.class);
    }

    // ------------------------------------------------------------------ deleteInventory

    @Test
    void deleteInventory_delegatesToRepository() {
        when(inventoryRepository.existsById(1L)).thenReturn(true);

        inventoryService.deleteInventory(1L);

        verify(inventoryRepository).deleteById(1L);
    }

    @Test
    void deleteInventory_throws_whenNotFound() {
        when(inventoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryService.deleteInventory(99L))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("99");
        verify(inventoryRepository, never()).deleteById(anyLong());
    }

    // ------------------------------------------------------------------ helpers

    private InventoryDTO buildDTO(Long id, Long productId, String sku,
                                  int qty, int reserved, int threshold) {
        return InventoryDTO.builder()
                .id(id)
                .productId(productId)
                .sku(sku)
                .quantityAvailable(qty)
                .reservedQuantity(reserved)
                .reorderThreshold(threshold)
                .warehouseLocation("WH-A")
                .shelfLocation("S-01")
                .build();
    }
}
