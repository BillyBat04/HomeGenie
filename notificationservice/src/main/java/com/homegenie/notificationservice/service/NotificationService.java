package com.homegenie.notificationservice.service;

import com.homegenie.notificationservice.dto.InvoiceEvent;
import com.homegenie.notificationservice.dto.MaintenanceReminderEvent;
import com.homegenie.notificationservice.dto.PaymentEvent;
import com.homegenie.notificationservice.dto.UserEvent;
import com.homegenie.notificationservice.dto.WarrantyExpiringEvent;
import com.homegenie.notificationservice.model.Notification;

import java.util.List;

public interface NotificationService {

    void sendPaymentConfirmation(PaymentEvent event);

    void sendPaymentFailed(PaymentEvent event);

    void sendRefundProcessed(PaymentEvent event);

    void sendInvoiceSent(InvoiceEvent event);

    void sendInvoiceOverdue(InvoiceEvent event);

    void sendInvoicePaid(InvoiceEvent event);

    void retryFailedNotifications();

    void cleanupOldNotifications();

    List<Notification> getNotificationsByUser(Long userId);

    List<Notification> getUnreadNotificationsByUser(Long userId);

    void markAsRead(Long notificationId);

    void sendWelcomeEmail(UserEvent event);

    void sendMaintenanceReminder(MaintenanceReminderEvent event);

    void sendWarrantyExpiryAlert(WarrantyExpiringEvent event);
}
