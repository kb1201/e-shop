package hr.fer.dipl.db.repository;


import hr.fer.dipl.db.model.Inventory;
import hr.fer.dipl.db.model.InventoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    Optional<Inventory> findBySku(String sku);

    List<Inventory> findByStatus(InventoryStatus status);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable - i.reservedQuantity <= i.reorderThreshold")
    List<Inventory> findItemsNeedingRestock();

    // Using pessimistic lock for concurrent inventory operations
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdWithLock(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(Long productId);
}