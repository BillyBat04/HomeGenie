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

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByUserId(Long userId);

    List<Item> findByUserIdAndStatus(Long userId, ItemStatus status);

    List<Item> findByUserIdAndCategory(Long userId, ItemCategory category);

    Optional<Item> findByIdAndUserId(Long id, Long userId);
 
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT i FROM Item i WHERE i.nextMaintenanceDate <= :today " +
           "AND i.status = 'ACTIVE' " +
           "ORDER BY i.nextMaintenanceDate ASC")
    List<Item> findItemsDueForMaintenance(@Param("today") LocalDate today);

    @Query("SELECT i FROM Item i WHERE i.nextMaintenanceDate BETWEEN :startDate AND :endDate " +
           "AND i.status = 'ACTIVE' " +
           "ORDER BY i.nextMaintenanceDate ASC")
    List<Item> findItemsMaintenanceDueSoon(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    @Query("SELECT i FROM Item i WHERE i.warrantyExpiryDate BETWEEN :today AND :expiryDate " +
           "AND i.status IN ('ACTIVE', 'INACTIVE') " +
           "ORDER BY i.warrantyExpiryDate ASC")
    List<Item> findItemsWithWarrantyExpiringSoon(
        @Param("today") LocalDate today,
        @Param("expiryDate") LocalDate expiryDate
    );
    @Query("SELECT i FROM Item i WHERE i.warrantyExpiryDate < :today " +
           "AND i.status IN ('ACTIVE', 'INACTIVE') " +
           "ORDER BY i.warrantyExpiryDate DESC")
    List<Item> findItemsWithExpiredWarranty(@Param("today") LocalDate today);

    @Query("SELECT i FROM Item i WHERE i.warrantyExpiryDate > :today " +
           "AND i.status IN ('ACTIVE', 'INACTIVE') " +
           "ORDER BY i.warrantyExpiryDate ASC")
    List<Item> findItemsWithValidWarranty(@Param("today") LocalDate today);

    List<Item> findByStatus(ItemStatus status);

    List<Item> findByUserIdAndStatusAndCategory(Long userId, ItemStatus status, ItemCategory category);

    long countByUserIdAndStatus(Long userId, ItemStatus status);

    long countByUserIdAndCategory(Long userId, ItemCategory category);

    boolean existsByIdAndUserId(Long id, Long userId);
}
