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

    /**
     * Idempotency check: returns true if a notification with this Kafka eventId was already saved.
     * Call this before saving to prevent duplicate processing of re-delivered Kafka messages.
     */
    boolean existsByEventId(String eventId);
    
    // Find notifications that need retry
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < n.maxRetries AND n.failedAt > :afterTime")
    List<Notification> findFailedNotificationsForRetry(LocalDateTime afterTime);
    
    // Find pending notifications older than specified time
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.createdAt < :beforeTime")
    List<Notification> findPendingNotificationsOlderThan(LocalDateTime beforeTime);
    
    // Count notifications by status for monitoring
    Long countByStatus(NotificationStatus status);
    
    // Find notifications in date range
    List<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Delete old notifications (cleanup job)
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
    
    // Find unread notifications for user
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.status != 'READ' ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByUser(Long userId);
    
    // Mini-app platform queries (v2)
    List<Notification> findByMiniAppId(String miniAppId);
    List<Notification> findByUserIdAndMiniAppId(Long userId, String miniAppId);
    List<Notification> findByMiniAppIdAndStatus(String miniAppId, NotificationStatus status);
    long countByMiniAppIdAndStatus(String miniAppId, NotificationStatus status);
}
