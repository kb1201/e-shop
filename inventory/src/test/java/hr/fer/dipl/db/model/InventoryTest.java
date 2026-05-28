package hr.fer.dipl.db.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class InventoryTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .id(1L)
                .productId(10L)
                .sku("SKU-001")
                .quantityAvailable(100)
                .reservedQuantity(0)
                .reorderThreshold(10)
                .warehouseLocation("WH-A")
                .shelfLocation("S-01")
                .build();
    }

    // ------------------------------------------------------------------ updateStatus

    @Test
    void updateStatus_inStock_whenAvailableWellAboveThreshold() {
        inventory.setQuantityAvailable(100);
        inventory.setReservedQuantity(0);
        inventory.setReorderThreshold(10);

        inventory.updateStatus();

        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.IN_STOCK);
    }

    @Test
    void updateStatus_lowStock_whenAvailableEqualsThreshold() {
        inventory.setQuantityAvailable(20);
        inventory.setReservedQuantity(10);  // net = 10 == threshold
        inventory.setReorderThreshold(10);

        inventory.updateStatus();

        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.LOW_STOCK);
    }

    @Test
    void updateStatus_lowStock_whenAvailableBelowThresholdButPositive() {
        inventory.setQuantityAvailable(15);
        inventory.setReservedQuantity(10);  // net = 5 < threshold 10
        inventory.setReorderThreshold(10);

        inventory.updateStatus();

        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.LOW_STOCK);
    }

    @Test
    void updateStatus_outOfStock_whenNetQuantityIsZero() {
        inventory.setQuantityAvailable(5);
        inventory.setReservedQuantity(5);  // net = 0
        inventory.setReorderThreshold(10);

        inventory.updateStatus();

        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.OUT_OF_STOCK);
    }

    @Test
    void updateStatus_outOfStock_whenNetQuantityIsNegative() {
        inventory.setQuantityAvailable(3);
        inventory.setReservedQuantity(5);  // net = -2
        inventory.setReorderThreshold(10);

        inventory.updateStatus();

        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.OUT_OF_STOCK);
    }

    // ------------------------------------------------------------------ canReserve

    @Test
    void canReserve_returnsTrue_whenEnoughAvailable() {
        inventory.setQuantityAvailable(50);
        inventory.setReservedQuantity(10);  // net = 40

        assertThat(inventory.canReserve(40)).isTrue();
    }

    @Test
    void canReserve_returnsTrue_whenExactlyEnough() {
        inventory.setQuantityAvailable(50);
        inventory.setReservedQuantity(30);  // net = 20

        assertThat(inventory.canReserve(20)).isTrue();
    }

    @Test
    void canReserve_returnsFalse_whenNotEnoughAvailable() {
        inventory.setQuantityAvailable(50);
        inventory.setReservedQuantity(40);  // net = 10

        assertThat(inventory.canReserve(11)).isFalse();
    }

    @Test
    void canReserve_returnsFalse_whenFullyReserved() {
        inventory.setQuantityAvailable(10);
        inventory.setReservedQuantity(10);  // net = 0

        assertThat(inventory.canReserve(1)).isFalse();
    }

    // ------------------------------------------------------------------ reserve

    @Test
    void reserve_increasesReservedQuantity() {
        inventory.setQuantityAvailable(100);
        inventory.setReservedQuantity(0);

        inventory.reserve(30);

        assertThat(inventory.getReservedQuantity()).isEqualTo(30);
    }

    @Test
    void reserve_updatesStatusAfterReserve() {
        inventory.setQuantityAvailable(15);
        inventory.setReservedQuantity(0);
        inventory.setReorderThreshold(10);  // net after = 15-10 = 5 â†’ LOW_STOCK

        inventory.reserve(10);

        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.LOW_STOCK);
    }

    @Test
    void reserve_throws_whenNotEnoughInventory() {
        inventory.setQuantityAvailable(10);
        inventory.setReservedQuantity(5);  // net = 5

        assertThatThrownBy(() -> inventory.reserve(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot reserve more than available");
    }

    // ------------------------------------------------------------------ releaseReservation

    @Test
    void releaseReservation_decreasesReservedQuantity() {
        inventory.setQuantityAvailable(100);
        inventory.setReservedQuantity(20);

        inventory.releaseReservation(10);

        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
    }

    @Test
    void releaseReservation_updatesStatusAfterRelease() {
        inventory.setQuantityAvailable(20);
        inventory.setReservedQuantity(20);  // net before = 0 â†’ OUT_OF_STOCK
        inventory.setReorderThreshold(5);
        inventory.updateStatus();
        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.OUT_OF_STOCK);

        inventory.releaseReservation(15);  // net after = 15 > threshold 5 â†’ IN_STOCK

        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.IN_STOCK);
    }

    @Test
    void releaseReservation_throws_whenReleasingMoreThanReserved() {
        inventory.setReservedQuantity(5);

        assertThatThrownBy(() -> inventory.releaseReservation(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot release more than reserved");
    }

    // ------------------------------------------------------------------ commitReservation

    @Test
    void commitReservation_decreasesBothQuantities() {
        inventory.setQuantityAvailable(100);
        inventory.setReservedQuantity(30);

        inventory.commitReservation(20);

        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(80);
    }

    @Test
    void commitReservation_throws_whenCommittingMoreThanReserved() {
        inventory.setQuantityAvailable(50);
        inventory.setReservedQuantity(5);

        assertThatThrownBy(() -> inventory.commitReservation(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot commit more than reserved");
    }

    @Test
    void commitReservation_updatesStatusAfterCommit() {
        inventory.setQuantityAvailable(15);
        inventory.setReservedQuantity(10);
        inventory.setReorderThreshold(10);

        inventory.commitReservation(10);  // available = 5, reserved = 0, net = 5 <= threshold â†’ LOW_STOCK

        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.LOW_STOCK);
    }

    // ------------------------------------------------------------------ restock

    @Test
    void restock_increasesQuantityAvailable() {
        inventory.setQuantityAvailable(10);

        inventory.restock(50);

        assertThat(inventory.getQuantityAvailable()).isEqualTo(60);
    }

    @Test
    void restock_updatesStatusAfterRestock() {
        inventory.setQuantityAvailable(5);
        inventory.setReservedQuantity(5);  // net = 0 â†’ OUT_OF_STOCK
        inventory.setReorderThreshold(10);
        inventory.updateStatus();
        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.OUT_OF_STOCK);

        inventory.restock(100);  // net = 100 > threshold â†’ IN_STOCK

        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.IN_STOCK);
    }
}
