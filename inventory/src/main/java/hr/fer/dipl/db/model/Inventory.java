package hr.fer.dipl.db.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data // Includes @Getter, @Setter, @ToString, @EqualsAndHashCode, and @RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
@Builder // Optional: for fluent object construction
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private Integer quantityAvailable;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Column(nullable = false)
    private Integer reorderThreshold;

    private String warehouseLocation;
    private String shelfLocation;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Enumerated(EnumType.STRING)
    private InventoryStatus status;

    // Custom constructor for required fields
    public Inventory(Long productId, String sku, Integer quantityAvailable, 
                     Integer reorderThreshold, String warehouseLocation, String shelfLocation) {
        this.productId = productId;
        this.sku = sku;
        this.quantityAvailable = quantityAvailable;
        this.reservedQuantity = 0;
        this.reorderThreshold = reorderThreshold;
        this.warehouseLocation = warehouseLocation;
        this.shelfLocation = shelfLocation;
        this.lastUpdated = LocalDateTime.now();
        updateStatus();
    }

    @PrePersist
    @PreUpdate
    public void setTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }

    public void updateStatus() {
        int availableForSale = quantityAvailable - reservedQuantity;
        if (availableForSale <= 0) {
            this.status = InventoryStatus.OUT_OF_STOCK;
        } else if (availableForSale <= reorderThreshold) {
            this.status = InventoryStatus.LOW_STOCK;
        } else {
            this.status = InventoryStatus.IN_STOCK;
        }
    }

    public boolean canReserve(int quantity) {
        return (quantityAvailable - reservedQuantity) >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalArgumentException("Cannot reserve more than available quantity");
        }
        this.reservedQuantity += quantity;
        setTimestamp();
        updateStatus();
    }

    public void releaseReservation(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalArgumentException("Cannot release more than reserved quantity");
        }
        this.reservedQuantity -= quantity;
        setTimestamp();
        updateStatus();
    }

    public void commitReservation(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalArgumentException("Cannot commit more than reserved quantity");
        }
        this.reservedQuantity -= quantity;
        this.quantityAvailable -= quantity;
        setTimestamp();
        updateStatus();
    }

    public void restock(int quantity) {
        this.quantityAvailable += quantity;
        setTimestamp();
        updateStatus();
    }

}
