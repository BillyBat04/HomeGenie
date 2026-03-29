package com.homegenie.notificationservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EmailService {

    void sendEmail(String to, String subject, String htmlBody);

    String buildPaymentConfirmationEmail(String userName, BigDecimal amount, String currency,
                                         String paymentMethod, Long paymentId, String receiptUrl);

    String buildPaymentFailedEmail(String userName, BigDecimal amount, String currency,
                                   String failureReason, Long paymentId);

    String buildRefundProcessedEmail(String userName, BigDecimal refundAmount, String currency,
                                     Long paymentId, String reason);

    String buildInvoiceSentEmail(String userName, String invoiceNumber, BigDecimal totalAmount,
                                 String currency, LocalDateTime dueDate, Long invoiceId);

    String buildInvoiceOverdueEmail(String userName, String invoiceNumber, BigDecimal totalAmount,
                                    String currency, BigDecimal lateFee, Long invoiceId);

    String buildInvoicePaidEmail(String userName, String invoiceNumber, BigDecimal paidAmount,
                                 String currency, LocalDateTime paidDate, Long invoiceId);

    String buildWelcomeEmail(String userName, String userRole);
}
