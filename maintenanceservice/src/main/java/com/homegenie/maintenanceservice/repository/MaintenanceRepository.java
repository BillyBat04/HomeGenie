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
    List<MaintenanceRequest> findByStatusAndPaymentStatusIn(Status status, List<PaymentStatus> paymentStatuses);
    List<MaintenanceRequest> findByItemIdOrderByCreatedAtDesc(Long itemId);
    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.itemId = :itemId " +
           "AND mr.status = 'COMPLETED' " +
           "ORDER BY mr.resolvedAt DESC")
    List<MaintenanceRequest> findCompletedMaintenanceForItem(@Param("itemId") Long itemId);
    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.itemId = :itemId " +
           "AND mr.status IN ('PENDING', 'IN_PROGRESS') " +
           "ORDER BY mr.createdAt DESC")
    List<MaintenanceRequest> findActiveMaintenanceForItem(@Param("itemId") Long itemId);
    long countByItemId(Long itemId);
    long countByItemIdAndStatus(Long itemId, Status status);
}