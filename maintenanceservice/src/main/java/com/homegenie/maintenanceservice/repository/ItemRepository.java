package com.homegenie.maintenanceservice.repository;

import com.homegenie.maintenanceservice.model.Item;
import com.homegenie.maintenanceservice.model.ItemCategory;
import com.homegenie.maintenanceservice.model.ItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Item entity
 * 
 * Query patterns:
 * - User's items (basic + filtered)
 * - Maintenance due (reminders)
 * - Warranty expiring (alerts)
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Find all items owned by a user
     */
    List<Item> findByUserId(Long userId);

    /**
     * Find all active items owned by a user
     */
    List<Item> findByUserIdAndStatus(Long userId, ItemStatus status);

    /**
     * Find items by user and category
     */
    List<Item> findByUserIdAndCategory(Long userId, ItemCategory category);

    /**
     * Find user's item by ID (ensures ownership)
     */
    Optional<Item> findByIdAndUserId(Long id, Long userId);
    
    /**
     * Find item by ID with pessimistic lock (for scheduler updates)
     * Prevents race conditions when multiple scheduler threads update same item
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdWithLock(@Param("id") Long id);

    /**
     * Find items needing maintenance (nextMaintenanceDate <= today, status = ACTIVE)
     * Critical query for scheduler
     */
    @Query("SELECT i FROM Item i WHERE i.nextMaintenanceDate <= :today " +
           "AND i.status = 'ACTIVE' " +
           "ORDER BY i.nextMaintenanceDate ASC")
    List<Item> findItemsDueForMaintenance(@Param("today") LocalDate today);

    /**
     * Find items with maintenance due soon (within X days)
     * Used for reminder notifications
     */
    @Query("SELECT i FROM Item i WHERE i.nextMaintenanceDate BETWEEN :startDate AND :endDate " +
           "AND i.status = 'ACTIVE' " +
           "ORDER BY i.nextMaintenanceDate ASC")
    List<Item> findItemsMaintenanceDueSoon(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find items with warranty expiring soon (within X days from today)
     * Used for warranty expiration alerts
     */
    @Query("SELECT i FROM Item i WHERE i.warrantyExpiryDate BETWEEN :today AND :expiryDate " +
           "AND i.status IN ('ACTIVE', 'INACTIVE') " +
           "ORDER BY i.warrantyExpiryDate ASC")
    List<Item> findItemsWithWarrantyExpiringSoon(
        @Param("today") LocalDate today,
        @Param("expiryDate") LocalDate expiryDate
    );

    /**
     * Find items where warranty has already expired
     */
    @Query("SELECT i FROM Item i WHERE i.warrantyExpiryDate < :today " +
           "AND i.status IN ('ACTIVE', 'INACTIVE') " +
           "ORDER BY i.warrantyExpiryDate DESC")
    List<Item> findItemsWithExpiredWarranty(@Param("today") LocalDate today);

    /**
     * Find items with valid warranty
     */
    @Query("SELECT i FROM Item i WHERE i.warrantyExpiryDate > :today " +
           "AND i.status IN ('ACTIVE', 'INACTIVE') " +
           "ORDER BY i.warrantyExpiryDate ASC")
    List<Item> findItemsWithValidWarranty(@Param("today") LocalDate today);

    /**
     * Find items under repair
     */
    List<Item> findByStatus(ItemStatus status);

    /**
     * Find user's items under repair
     */
    List<Item> findByUserIdAndStatusAndCategory(Long userId, ItemStatus status, ItemCategory category);

    /**
     * Count items by user and status
     */
    long countByUserIdAndStatus(Long userId, ItemStatus status);

    /**
     * Count items by user and category
     */
    long countByUserIdAndCategory(Long userId, ItemCategory category);

    /**
     * Check if user owns the item
     */
    boolean existsByIdAndUserId(Long id, Long userId);
}
