package com.homegenie.maintenanceservice.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    // 🔧 Common properties
    @Value("${email.provider:smtp}") // default = smtp
    private String emailProvider;

    @Value("${spring.mail.username:no-reply@homegenie.com}")
    private String smtpFromEmail;

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.ses.from-email:no-reply@homegenie.com}")
    private String sesFromEmail;

    @Value("${admin.email:admin@homegenie.com}")
    private String adminEmail;

    private AmazonSimpleEmailService sesClient;

    public void notifyAdminNewRequest(String userName, String requestTitle,
                                      String category, String priority, Long requestId) {
        String subject = "🔔 New Maintenance Request - " + priority + " Priority";
        String htmlBody = buildNewRequestEmailHtml(userName, requestTitle, category, priority, requestId);
        sendEmail(adminEmail, subject, htmlBody);
    }

    public void notifyTechnicianAssignment(String technicianEmail, String technicianName,
                                           String requestTitle, String category,
                                           String priority, Long requestId) {
        String subject = "🔧 New Request Assigned to You";
        String htmlBody = buildAssignmentEmailHtml(technicianName, requestTitle, category, priority, requestId);
        sendEmail(technicianEmail, subject, htmlBody);
    }

    public void notifyResidentStatusChange(String residentEmail, String residentName,
                                           String requestTitle, String oldStatus,
                                           String newStatus, Long requestId) {
        String subject = "📢 Request Status Updated: " + requestTitle;
        String htmlBody = buildStatusChangeEmailHtml(residentName, requestTitle, oldStatus, newStatus, requestId);
        sendEmail(residentEmail, subject, htmlBody);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            if ("ses".equalsIgnoreCase(emailProvider)) {
                sendViaSES(to, subject, htmlBody);
            } else {
                sendViaSMTP(to, subject, htmlBody);
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
        }
    }

    private void sendViaSMTP(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(smtpFromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
        log.info("✅ SMTP email sent to {}", to);
    }

    private void sendViaSES(String to, String subject, String htmlBody) {
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
                .withSource(sesFromEmail);

        sesClient.sendEmail(request);
        log.info("✅ AWS SES email sent to {}", to);
    }

    private String buildNewRequestEmailHtml(String userName, String requestTitle,
                                            String category, String priority, Long requestId) {
        String priorityColor = getPriorityColor(priority);
        return String.format("""
            <html>
            <body>
                <h2>🏠 HomeGenie - New Maintenance Request</h2>
                <p><b>Request ID:</b> #%d</p>
                <p><b>User:</b> %s</p>
                <p><b>Title:</b> %s</p>
                <p><b>Category:</b> %s</p>
                <p><b>Priority:</b> <span style="color:%s;">%s</span></p>
                <a href="http://localhost:3000">View Dashboard</a>
            </body>
            </html>
            """, requestId, userName, requestTitle, category, priorityColor, priority);
    }

    private String buildAssignmentEmailHtml(String technicianName, String requestTitle,
                                            String category, String priority, Long requestId) {
        String priorityColor = getPriorityColor(priority);
        return String.format("""
            <html>
            <body>
                <h2>🔧 Maintenance Request Assigned</h2>
                <p>Hello %s,</p>
                <p>Request #%d has been assigned to you.</p>
                <p><b>Title:</b> %s</p>
                <p><b>Category:</b> %s</p>
                <p><b>Priority:</b> <span style="color:%s;">%s</span></p>
                <a href="http://localhost:3000">View Details</a>
            </body>
            </html>
            """, technicianName, requestId, requestTitle, category, priorityColor, priority);
    }

    private String buildStatusChangeEmailHtml(String residentName, String requestTitle,
                                              String oldStatus, String newStatus, Long requestId) {
        return String.format("""
            <html>
            <body>
                <h2>📢 Request Status Updated</h2>
                <p>Hello %s,</p>
                <p>Your request <b>%s</b> (ID #%d) has been updated:</p>
                <p><b>Old Status:</b> %s → <b>New Status:</b> %s</p>
                <a href="http://localhost:3000">View Dashboard</a>
            </body>
            </html>
            """, residentName, requestTitle, requestId, oldStatus, newStatus);
    }

    private String getPriorityColor(String priority) {
        return switch (priority) {
            case "CRITICAL" -> "#e53e3e";
            case "HIGH" -> "#ed8936";
            case "MODERATE" -> "#ecc94b";
            case "LOW" -> "#48bb78";
            default -> "#718096";
        };
    }
}
