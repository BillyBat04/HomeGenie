package com.homegenie.notificationservice.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.provider:smtp}")
    private String emailProvider;

    @Value("${email.from:no-reply@homegenie.com}")
    private String fromEmail;

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Value("${aws.region:us-east-1}")
    private String region;

    private AmazonSimpleEmailService sesClient;

        public void sendEmail(String to, String subject, String htmlBody) {
        try {
            if ("ses".equalsIgnoreCase(emailProvider)) {
                sendViaSES(to, subject, htmlBody);
            } else {
                sendViaSMTP(to, subject, htmlBody);
            }
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    @SuppressWarnings("null")
    private void sendViaSMTP(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    private synchronized void sendViaSES(String to, String subject, String htmlBody) {
        if (sesClient == null) {
            BasicAWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
            sesClient = AmazonSimpleEmailServiceClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(creds))
                    .withRegion(Regions.fromName(region))
                    .build();
        }

        SendEmailRequest request = new SendEmailRequest()
                .withDestination(new Destination().withToAddresses(to))
                .withMessage(new Message()
                        .withSubject(new Content().withCharset("UTF-8").withData(subject))
                        .withBody(new Body().withHtml(new Content().withCharset("UTF-8").withData(htmlBody))))
                .withSource(fromEmail);

        sesClient.sendEmail(request);
    }

    
    
    

    public String buildPaymentConfirmationEmail(String userName, BigDecimal amount, String currency,
                                                String paymentMethod, Long paymentId, String receiptUrl) {
        String amountFormatted = String.format("%s %.2f", currency.toUpperCase(), amount);
        String receiptLink = receiptUrl != null 
            ? String.format("<p><a href=\"%s\" style=\"background:#4CAF50;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;display:inline-block;margin-top:20px;\">View Receipt</a></p>", receiptUrl)
            : "";

        return String.format("""
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .footer { background: #333; color: white; padding: 15px; text-align: center; font-size: 12px; border-radius: 0 0 5px 5px; }
                    .success-icon { font-size: 48px; color: #4CAF50; }
                    .detail-row { margin: 15px 0; padding: 10px; background: white; border-left: 3px solid #4CAF50; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Payment Successful</h1>
                    </div>
                    <div class="content">
                        <div class="success-icon" style="text-align:center;">✓</div>
                        <h2 style="color:#4CAF50;text-align:center;">Payment Confirmed</h2>
                        <p>Dear %s,</p>
                        <p>Your payment has been processed successfully!</p>
                        
                        <div class="detail-row">
                            <b>Amount:</b> %s
                        </div>
                        <div class="detail-row">
                            <b>Payment Method:</b> %s
                        </div>
                        <div class="detail-row">
                            <b>Payment ID:</b> #%d
                        </div>
                        <div class="detail-row">
                            <b>Date:</b> %s
                        </div>
                        
                        %s
                        
                        <p style="margin-top:30px;color:#666;">Thank you for your payment!</p>
                    </div>
                    <div class="footer">
                        <p>HomeGenie - Smart Maintenance Management</p>
                        <p>This is an automated message, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, amountFormatted, formatPaymentMethod(paymentMethod), paymentId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                receiptLink);
    }

    public String buildPaymentFailedEmail(String userName, BigDecimal amount, String currency,
                                          String failureReason, Long paymentId) {
        String amountFormatted = String.format("%s %.2f", currency.toUpperCase(), amount);
        
        return String.format("""
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #f44336; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .footer { background: #333; color: white; padding: 15px; text-align: center; font-size: 12px; border-radius: 0 0 5px 5px; }
                    .error-icon { font-size: 48px; color: #f44336; }
                    .retry-btn { background: #2196F3; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1> Payment Failed</h1>
                    </div>
                    <div class="content">
                        <div class="error-icon" style="text-align:center;">✗</div>
                        <h2 style="color:#f44336;text-align:center;">Payment Could Not Be Processed</h2>
                        <p>Dear %s,</p>
                        <p>Unfortunately, your payment could not be completed.</p>
                        
                        <div style="margin:20px 0;padding:15px;background:#fff3cd;border-left:4px solid #f44336;">
                            <b>Amount:</b> %s<br>
                            <b>Payment ID:</b> #%d<br>
                            <b>Reason:</b> %s
                        </div>
                        
                        <p><b>What to do next:</b></p>
                        <ul>
                            <li>Check your payment method details</li>
                            <li>Ensure sufficient funds are available</li>
                            <li>Try a different payment method</li>
                            <li>Contact your bank if the issue persists</li>
                        </ul>
                        
                        <a href="http:
                    </div>
                    <div class="footer">
                        <p>HomeGenie - Smart Maintenance Management</p>
                        <p>Need help? Contact support@homegenie.com</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, amountFormatted, paymentId, failureReason);
    }

    public String buildRefundProcessedEmail(String userName, BigDecimal refundAmount, String currency,
                                            Long paymentId, String reason) {
        String amountFormatted = String.format("%s %.2f", currency.toUpperCase(), refundAmount);
        
        return String.format("""
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #2196F3; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .footer { background: #333; color: white; padding: 15px; text-align: center; font-size: 12px; border-radius: 0 0 5px 5px; }
                    .info-box { background: #e3f2fd; padding: 15px; border-left: 4px solid #2196F3; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Refund Processed</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p>Your refund has been processed successfully.</p>
                        
                        <div class="info-box">
                            <b>Refund Amount:</b> %s<br>
                            <b>Original Payment ID:</b> #%d<br>
                            <b>Reason:</b> %s<br>
                            <b>Processing Time:</b> 5-10 business days
                        </div>
                        
                        <p>The refund will be credited to your original payment method within 5-10 business days.</p>
                        <p>If you don't see the refund after this period, please contact your bank.</p>
                    </div>
                    <div class="footer">
                        <p>HomeGenie - Smart Maintenance Management</p>
                        <p>Questions? Contact support@homegenie.com</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, amountFormatted, paymentId, reason);
    }

    
    
    

    public String buildInvoiceSentEmail(String userName, String invoiceNumber, BigDecimal totalAmount,
                                        String currency, LocalDateTime dueDate, Long invoiceId) {
        String amountFormatted = String.format("%s %.2f", currency.toUpperCase(), totalAmount);
        String dueDateFormatted = dueDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        
        return String.format("""
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #673AB7; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .footer { background: #333; color: white; padding: 15px; text-align: center; font-size: 12px; border-radius: 0 0 5px 5px; }
                    .invoice-box { background: white; padding: 20px; border: 2px solid #673AB7; border-radius: 5px; margin: 20px 0; }
                    .pay-btn { background: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>New Invoice</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p>You have received a new invoice from HomeGenie.</p>
                        
                        <div class="invoice-box">
                            <h3 style="margin-top:0;color:#673AB7;">Invoice Details</h3>
                            <p><b>Invoice Number:</b> %s</p>
                            <p><b>Total Amount:</b> <span style="font-size:24px;color:#4CAF50;">%s</span></p>
                            <p><b>Due Date:</b> %s</p>
                            <p><b>Invoice ID:</b> #%d</p>
                        </div>
                        
                        <p>Please make payment before the due date to avoid late fees.</p>
                        
                        <a href="http:
                    </div>
                    <div class="footer">
                        <p>HomeGenie - Smart Maintenance Management</p>
                        <p>Questions about this invoice? Contact billing@homegenie.com</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, invoiceNumber, amountFormatted, dueDateFormatted, invoiceId, invoiceId);
    }

    public String buildInvoiceOverdueEmail(String userName, String invoiceNumber, BigDecimal totalAmount,
                                           String currency, BigDecimal lateFee, Long invoiceId) {
        String amountFormatted = String.format("%s %.2f", currency.toUpperCase(), totalAmount);
        String lateFeeFormatted = String.format("%s %.2f", currency.toUpperCase(), lateFee);
        
        return String.format("""
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #FF5722; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .footer { background: #333; color: white; padding: 15px; text-align: center; font-size: 12px; border-radius: 0 0 5px 5px; }
                    .warning-box { background: #fff3e0; padding: 20px; border-left: 4px solid #FF5722; margin: 20px 0; }
                    .urgent-btn { background: #FF5722; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Invoice Overdue</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p><b>Your invoice is now overdue.</b></p>
                        
                        <div class="warning-box">
                            <h3 style="margin-top:0;color:#FF5722;">Overdue Invoice</h3>
                            <p><b>Invoice Number:</b> %s</p>
                            <p><b>Original Amount:</b> %s</p>
                            <p><b>Late Fee:</b> %s</p>
                            <p><b>Total Due:</b> <span style="font-size:24px;color:#FF5722;">%s</span></p>
                        </div>
                        
                        <p><b>Please pay immediately to avoid additional charges.</b></p>
                        
                        <p>If you've already paid, please disregard this notice.</p>
                        
                        <a href="http:
                    </div>
                    <div class="footer">
                        <p>HomeGenie - Smart Maintenance Management</p>
                        <p>Payment issues? Contact billing@homegenie.com</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, invoiceNumber, amountFormatted, lateFeeFormatted,
                String.format("%s %.2f", currency.toUpperCase(), totalAmount.add(lateFee)), invoiceId);
    }

    public String buildInvoicePaidEmail(String userName, String invoiceNumber, BigDecimal paidAmount,
                                        String currency, LocalDateTime paidDate, Long invoiceId) {
        String amountFormatted = String.format("%s %.2f", currency.toUpperCase(), paidAmount);
        String dateFormatted = paidDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        
        return String.format("""
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                    .footer { background: #333; color: white; padding: 15px; text-align: center; font-size: 12px; border-radius: 0 0 5px 5px; }
                    .success-box { background: #e8f5e9; padding: 20px; border-left: 4px solid #4CAF50; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Invoice Paid</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p>Thank you! Your invoice has been marked as paid.</p>
                        
                        <div class="success-box">
                            <h3 style="margin-top:0;color:#4CAF50;">Payment Received</h3>
                            <p><b>Invoice Number:</b> %s</p>
                            <p><b>Amount Paid:</b> %s</p>
                            <p><b>Payment Date:</b> %s</p>
                            <p><b>Invoice ID:</b> #%d</p>
                        </div>
                        
                        <p>Your account is now up to date. Thank you for your prompt payment!</p>
                    </div>
                    <div class="footer">
                        <p>HomeGenie - Smart Maintenance Management</p>
                        <p>Keep this email for your records</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, invoiceNumber, amountFormatted, dateFormatted, invoiceId);
    }

    
    
    

    private String formatPaymentMethod(String method) {
        return switch (method) {
            case "CREDIT_CARD" -> "Credit Card";
            case "DEBIT_CARD" -> "Debit Card";
            case "UPI" -> "UPI";
            case "NET_BANKING" -> "Net Banking";
            case "WALLET" -> "Digital Wallet";
            default -> method;
        };
    }

    
    
    

    public String buildWelcomeEmail(String userName, String userRole) {
        String roleMessage = switch (userRole) {
            case "RESIDENT" -> "As a resident, you can now submit maintenance requests, track their progress, and manage your home services.";
            case "TECHNICIAN" -> "As a technician, you can view assigned maintenance requests and update their status.";
            case "ADMIN" -> "As an administrator, you have full access to manage users, maintenance requests, and system settings.";
            default -> "Welcome to our platform!";
        };

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .highlight { background: #667eea; color: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    .button { background: #667eea; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 20px; }
                    .features { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .feature-item { padding: 10px 0; border-bottom: 1px solid #eee; }
                    .feature-item:last-child { border-bottom: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to HomeGenie!</h1>
                        <p>Your smart home maintenance platform</p>
                    </div>
                    <div class="content">
                        <h2>Hello, %s!</h2>
                        <p>Thank you for registering with HomeGenie. Your account has been successfully created!</p>
                        
                        <div class="highlight">
                            <h3>Your Role: %s</h3>
                            <p>%s</p>
                        </div>
                        
                        <div class="features">
                            <h3>Key Features</h3>
                            <div class="feature-item">Submit and track maintenance requests</div>
                            <div class="feature-item">Real-time notifications</div>
                            <div class="feature-item">Integrated payment system</div>
                            <div class="feature-item">Email and SMS alerts</div>
                            <div class="feature-item">Service history tracking</div>
                        </div>
                        
                        <p>To get started, simply log in to your account and explore the platform.</p>
                        
                        <p style="text-align: center;">
                            <a href="http:
                        </p>
                        
                        <p style="margin-top: 30px; padding-top: 20px; border-top: 2px solid #eee;">
                            <strong>Need Help?</strong><br>
                            If you have any questions, feel free to reach out to our support team at support@homegenie.com
                        </p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 HomeGenie. All rights reserved.</p>
                        <p>This is an automated message. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, userRole, roleMessage);
    }
}
