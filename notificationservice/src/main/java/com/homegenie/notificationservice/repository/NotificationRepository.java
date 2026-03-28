package com.homegenie.notificationservice.repository;

import com.homegenie.notificationservice.model.Notification;
import com.homegenie.notificationservice.model.Notification.NotificationStatus;
import com.homegenie.notificationservice.model.Notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);
    
    List<Notification> findByRecipient(String recipient);
    
    List<Notification> findByStatus(NotificationStatus status);
    
    List<Notification> findByType(NotificationType type);
    
    List<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status);
    
    List<Notification> findByPaymentId(Long paymentId);
    
    List<Notification> findByInvoiceId(Long invoiceId);
    
    List<Notification> findByRequestId(Long requestId);

    
    boolean existsByEventId(String eventId);
    
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < n.maxRetries")
    List<Notification> findFailedNotificationsForRetry();
    
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.createdAt < :beforeTime")
    List<Notification> findPendingNotificationsOlderThan(LocalDateTime beforeTime);
    
    
    Long countByStatus(NotificationStatus status);
    
    
    List<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
    
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.status != 'READ' ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByUser(Long userId);
    
    
    List<Notification> findByMiniAppId(String miniAppId);
    List<Notification> findByUserIdAndMiniAppId(Long userId, String miniAppId);
    List<Notification> findByMiniAppIdAndStatus(String miniAppId, NotificationStatus status);
    long countByMiniAppIdAndStatus(String miniAppId, NotificationStatus status);
}
