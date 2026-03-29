package com.homegenie.notificationservice.service.impl;


import com.homegenie.notificationservice.service.EmailService;
import com.homegenie.notificationservice.service.NotificationService;
import com.homegenie.notificationservice.exception.NotificationNotFoundException;
import com.homegenie.notificationservice.dto.*;
import com.homegenie.notificationservice.model.Notification;
import com.homegenie.notificationservice.model.Notification.*;
import com.homegenie.notificationservice.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Value("${notification.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Async
    @Transactional
    public void sendPaymentConfirmation(PaymentEvent event) {
        log.info("💳 Sending payment confirmation for payment ID: {}", event.getPaymentId());
        
        try {
            UserResponse user = getUserDetails(event.getUserId());
            
            String htmlContent = emailService.buildPaymentConfirmationEmail(
                    user.getFullName(),
                    event.getAmount(),
                    event.getCurrency(),
                    event.getPaymentMethod(),
                    event.getPaymentId(),
                    event.getReceiptUrl()
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.PAYMENT_CONFIRMATION)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .recipient(user.getEmail())
                    .recipientName(user.getFullName())
                    .subject("Payment Confirmation - HomeGenie")
                    .htmlContent(htmlContent)
                    .userId(event.getUserId())
                    .requestId(event.getRequestId())
                    .paymentId(event.getPaymentId())
                    .templateId("payment-confirmation")
                    .maxRetries(maxRetryAttempts)
                    .build();

            notification = notificationRepository.save(notification);
            sendNotification(notification);

        } catch (Exception e) {
            log.error(" Failed to send payment confirmation: {}", e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void sendPaymentFailed(PaymentEvent event) {
        log.info("Sending payment failed notification for payment ID: {}", event.getPaymentId());
        
        try {
            UserResponse user = getUserDetails(event.getUserId());
            
            String htmlContent = emailService.buildPaymentFailedEmail(
                    user.getFullName(),
                    event.getAmount(),
                    event.getCurrency(),
                    event.getFailureReason(),
                    event.getPaymentId()
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.PAYMENT_FAILED)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .recipient(user.getEmail())
                    .recipientName(user.getFullName())
                    .subject("Payment Failed - HomeGenie")
                    .htmlContent(htmlContent)
                    .userId(event.getUserId())
                    .requestId(event.getRequestId())
                    .paymentId(event.getPaymentId())
                    .templateId("payment-failed")
                    .maxRetries(maxRetryAttempts)
                    .build();

            notification = notificationRepository.save(notification);
            sendNotification(notification);

        } catch (Exception e) {
            log.error("Failed to send payment failed notification: {}", e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void sendRefundProcessed(PaymentEvent event) {
        log.info("Sending refund processed notification for payment ID: {}", event.getPaymentId());
        
        try {
            UserResponse user = getUserDetails(event.getUserId());
            
            String htmlContent = emailService.buildRefundProcessedEmail(
                    user.getFullName(),
                    event.getAmount(),
                    event.getCurrency(),
                    event.getPaymentId(),
                    "Refund requested by user"
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.PAYMENT_REFUNDED)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .recipient(user.getEmail())
                    .recipientName(user.getFullName())
                    .subject("💰 Refund Processed - HomeGenie")
                    .htmlContent(htmlContent)
                    .userId(event.getUserId())
                    .requestId(event.getRequestId())
                    .paymentId(event.getPaymentId())
                    .templateId("refund-processed")
                    .maxRetries(maxRetryAttempts)
                    .build();

            notification = notificationRepository.save(notification);
            sendNotification(notification);

        } catch (Exception e) {
            log.error("Failed to send refund notification: {}", e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void sendInvoiceSent(InvoiceEvent event) {
        log.info("Sending invoice sent notification for invoice: {}", event.getInvoiceNumber());
        
        try {
            UserResponse user = getUserDetails(event.getUserId());
            
            String htmlContent = emailService.buildInvoiceSentEmail(
                    user.getFullName(),
                    event.getInvoiceNumber(),
                    event.getTotalAmount(),
                    "USD",
                    event.getDueDate(),
                    event.getInvoiceId()
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.INVOICE_SENT)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .recipient(user.getEmail())
                    .recipientName(user.getFullName())
                    .subject("New Invoice " + event.getInvoiceNumber() + " - HomeGenie")
                    .htmlContent(htmlContent)
                    .userId(event.getUserId())
                    .requestId(event.getRequestId())
                    .invoiceId(event.getInvoiceId())
                    .templateId("invoice-sent")
                    .maxRetries(maxRetryAttempts)
                    .build();

            notification = notificationRepository.save(notification);
            sendNotification(notification);

        } catch (Exception e) {
            log.error("Failed to send invoice sent notification: {}", e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void sendInvoiceOverdue(InvoiceEvent event) {
        log.info("Sending invoice overdue notification for invoice: {}", event.getInvoiceNumber());
        
        try {
            UserResponse user = getUserDetails(event.getUserId());
            
            String htmlContent = emailService.buildInvoiceOverdueEmail(
                    user.getFullName(),
                    event.getInvoiceNumber(),
                    event.getTotalAmount(),
                    "USD",
                    event.getLateFee(),
                    event.getInvoiceId()
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.INVOICE_OVERDUE)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .recipient(user.getEmail())
                    .recipientName(user.getFullName())
                    .subject("Invoice Overdue " + event.getInvoiceNumber() + " - HomeGenie")
                    .htmlContent(htmlContent)
                    .userId(event.getUserId())
                    .requestId(event.getRequestId())
                    .invoiceId(event.getInvoiceId())
                    .templateId("invoice-overdue")
                    .maxRetries(maxRetryAttempts)
                    .build();

            notification = notificationRepository.save(notification);
            sendNotification(notification);

        } catch (Exception e) {
            log.error("Failed to send invoice overdue notification: {}", e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void sendInvoicePaid(InvoiceEvent event) {
        log.info("Sending invoice paid notification for invoice: {}", event.getInvoiceNumber());
        
        try {
            UserResponse user = getUserDetails(event.getUserId());
            
            String htmlContent = emailService.buildInvoicePaidEmail(
                    user.getFullName(),
                    event.getInvoiceNumber(),
                    event.getTotalAmount(),
                    "USD",
                    event.getPaidDate(),
                    event.getInvoiceId()
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.INVOICE_PAID)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .recipient(user.getEmail())
                    .recipientName(user.getFullName())
                    .subject("Invoice Paid " + event.getInvoiceNumber() + " - HomeGenie")
                    .htmlContent(htmlContent)
                    .userId(event.getUserId())
                    .requestId(event.getRequestId())
                    .invoiceId(event.getInvoiceId())
                    .paymentId(event.getPaymentId())
                    .templateId("invoice-paid")
                    .maxRetries(maxRetryAttempts)
                    .build();

            notification = notificationRepository.save(notification);
            sendNotification(notification);

        } catch (Exception e) {
            log.error("Failed to send invoice paid notification: {}", e.getMessage(), e);
        }
    }

    
    
    

    private void sendNotification(Notification notification) {
        try {
            notification.setStatus(NotificationStatus.SENDING);
            notificationRepository.save(notification);

            if (notification.getChannel() == NotificationChannel.EMAIL) {
                emailService.sendEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getHtmlContent()
                );
            }

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            log.info("Notification sent successfully: ID {}", notification.getId());

        } catch (Exception e) {
            log.error("Failed to send notification ID {}: {}", notification.getId(), e.getMessage());
            
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepository.save(notification);
        }
    }

    
    
    

    @Scheduled(fixedDelay = 300000) 
    @Transactional
    public void retryFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository
                .findFailedNotificationsForRetry();

        log.info("🔄 Retrying {} failed notifications", failedNotifications.size());

        for (Notification notification : failedNotifications) {
            if (notification.getRetryCount() < notification.getMaxRetries()) {
                log.info("🔄 Retrying notification ID: {} (attempt {})", 
                        notification.getId(), notification.getRetryCount() + 1);
                notification.setStatus(NotificationStatus.RETRYING);
                notificationRepository.save(notification);
                sendNotification(notification);
            }
        }
    }

    
    
    

    @Scheduled(cron = "0 0 2 * * ?") 
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        notificationRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("🧹 Cleaned up notifications older than 90 days");
    }

    
    
    

    public List<Notification> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotificationsByUser(Long userId) {
        return notificationRepository.findUnreadNotificationsByUser(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    
    
    

    @Async
    @Transactional
    public void sendWelcomeEmail(UserEvent event) {
        log.info("👋 Sending welcome email for user: {}", event.getEmail());
        
        try {
            String htmlContent = emailService.buildWelcomeEmail(
                    event.getFullName(),
                    event.getRole()
            );

            Notification notification = Notification.builder()
                    .type(NotificationType.WELCOME_EMAIL)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .recipient(event.getEmail())
                    .recipientName(event.getFullName())
                    .subject("🏠 Welcome to HomeGenie - " + event.getFullName())
                    .htmlContent(htmlContent)
                    .userId(event.getUserId())
                    .templateId("welcome-email")
                    .maxRetries(maxRetryAttempts)
                    .build();

            notification = notificationRepository.save(notification);
            sendNotification(notification);

        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage(), e);
        }
    }

    
    
    

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserDetailsFallback")
    private UserResponse getUserDetails(Long userId) {
        String url = userServiceUrl + "/api/users/" + userId;
        return restTemplate.getForObject(url, UserResponse.class);
    }

    
    @SuppressWarnings("unused") 
    private UserResponse getUserDetailsFallback(Long userId, Exception ex) {
        log.warn("Circuit breaker OPEN for User Service. userId={}, cause={}", userId, ex.getMessage());
        UserResponse stub = new UserResponse();
        stub.setId(userId);
        stub.setFullName("HomeGenie User");
        stub.setEmail("no-reply@homegenie.com");
        return stub;
    }

    
    @Async
    @Transactional
    public void sendMaintenanceReminder(com.homegenie.notificationservice.dto.MaintenanceReminderEvent event) {
        log.info("🔧 Sending maintenance reminder: itemId={}, urgency={}", event.getItemId(), event.getUrgencyLevel());
        try {
            String subject = "Maintenance Reminder: " + event.getItemName() + " — HomeGenie";
            String htmlContent = String.format(
                "<p>Hi %s,</p><p>Your item <b>%s</b> (%s) is due for maintenance.</p>"
                + "<p><b>Status:</b> %s</p><p><b>Next maintenance date:</b> %s</p>"
                + "<p>Please schedule a service via the HomeGenie app.</p>",
                event.getUserName(), event.getItemName(), event.getItemCategory(),
                event.getUrgencyLevel(), event.getNextMaintenanceDate());

            Notification notification = Notification.builder()
                    .type(NotificationType.MAINTENANCE_REMINDER)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .recipient(event.getUserEmail())
                    .recipientName(event.getUserName())
                    .subject(subject)
                    .htmlContent(htmlContent)
                    .userId(event.getUserId())
                    .templateId("maintenance-reminder")
                    .eventId(event.getEventId())   
                    .maxRetries(maxRetryAttempts)
                    .build();

            notification = notificationRepository.save(notification);
            sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send maintenance reminder: {}", e.getMessage(), e);
        }
    }

    
    @Async
    @Transactional
    public void sendWarrantyExpiryAlert(com.homegenie.notificationservice.dto.WarrantyExpiringEvent event) {
        log.info("📋 Sending warranty expiry alert: itemId={}, urgency={}", event.getItemId(), event.getUrgencyLevel());
        try {
            String subject = "Warranty Alert: " + event.getItemName() + " expires soon — HomeGenie";
            String htmlContent = String.format(
                "<p>Hi %s,</p><p>The warranty for your <b>%s %s</b> (%s) is expiring.</p>"
                + "<p><b>Expires:</b> %s (%d days remaining)</p>"
                + "<p>Consider renewing or purchasing an extended warranty through HomeGenie.</p>",
                event.getUserName(), event.getItemBrand(), event.getItemModel(), event.getItemName(),
                event.getWarrantyExpiryDate(), event.getDaysUntilExpiry());

            Notification notification = Notification.builder()
                    .type(NotificationType.WARRANTY_EXPIRY_ALERT)
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.PENDING)
                    .recipient(event.getUserEmail())
                    .recipientName(event.getUserName())
                    .subject(subject)
                    .htmlContent(htmlContent)
                    .userId(event.getUserId())
                    .templateId("warranty-expiry")
                    .eventId(event.getEventId())   
                    .maxRetries(maxRetryAttempts)
                    .build();

            notification = notificationRepository.save(notification);
            sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send warranty expiry alert: {}", e.getMessage(), e);
        }
    }
}
