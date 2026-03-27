package com.homegenie.maintenanceservice.repository;

import com.homegenie.maintenanceservice.model.MaintenanceRequest;
import com.homegenie.maintenanceservice.model.PaymentStatus;
import com.homegenie.maintenanceservice.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByUserId(Long userId);
    List<MaintenanceRequest> findByStatus(Status status);
    List<MaintenanceRequest> findByAssignedTo(Long assignedTo);

    /**
     * Used by the payment retry scheduler to find COMPLETED requests whose payment
     * creation failed or was never attempted. The scheduler retries all of these.
     */
    List<MaintenanceRequest> findByStatusAndPaymentStatusIn(Status status, List<PaymentStatus> paymentStatuses);
    
    /**
     * Find all maintenance requests for a specific item
     * Ordered by creation date descending (most recent first)
     */
    List<MaintenanceRequest> findByItemIdOrderByCreatedAtDesc(Long itemId);
    
    /**
     * Find completed maintenance requests for an item (acts as history)
     */
    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.itemId = :itemId " +
           "AND mr.status = 'COMPLETED' " +
           "ORDER BY mr.resolvedAt DESC")
    List<MaintenanceRequest> findCompletedMaintenanceForItem(@Param("itemId") Long itemId);
    
    /**
     * Find active (not completed/rejected) requests for an item
     */
    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.itemId = :itemId " +
           "AND mr.status IN ('PENDING', 'IN_PROGRESS') " +
           "ORDER BY mr.createdAt DESC")
    List<MaintenanceRequest> findActiveMaintenanceForItem(@Param("itemId") Long itemId);
    
    /**
     * Count maintenance requests by item
     */
    long countByItemId(Long itemId);
    
    /**
     * Count completed maintenance by item
     */
    long countByItemIdAndStatus(Long itemId, Status status);
}