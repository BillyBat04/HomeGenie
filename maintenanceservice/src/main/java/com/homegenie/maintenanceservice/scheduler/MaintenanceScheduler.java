package com.homegenie.maintenanceservice.scheduler;

import com.homegenie.maintenanceservice.dto.UserResponse;
import com.homegenie.maintenanceservice.model.MaintenanceRequest;
import com.homegenie.maintenanceservice.model.Status;
import com.homegenie.maintenanceservice.repository.MaintenanceRepository;
import com.homegenie.maintenanceservice.service.EmailNotificationService;
import com.homegenie.maintenanceservice.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduler {

    private final MaintenanceRepository repository;
    private final EmailNotificationService emailService;
    private final MaintenanceService maintenanceService; // to fetch user details

    // Format: second, minute, hour, day of month, month, day of week
    @Scheduled(cron = "0 0 */6 * * *") // every 6 hours
    public void checkPendingRequests() {
        log.info("🔍 Running scheduled check for pending maintenance requests...");

        List<MaintenanceRequest> pendingRequests = repository.findByStatus(Status.PENDING);
        LocalDateTime now = LocalDateTime.now();

        for (MaintenanceRequest request : pendingRequests) {
            if (request.getCreatedAt() != null) {
                Duration duration = Duration.between(request.getCreatedAt(), now);
                long hoursPending = duration.toHours();

                if (hoursPending >= 24) {
                    try {
                        UserResponse user = maintenanceService.getUserDetails(request.getUserId());
                        log.info("Request {} pending for {} hours. Sending reminder email...", request.getId(), hoursPending);

                        emailService.notifyAdminNewRequest(
                                user.getFullName(),
                                request.getTitle(),
                                request.getCategory().toString(),
                                request.getPriority().toString(),
                                request.getId()
                        );

                        log.info("Reminder email sent for request ID: {}", request.getId());
                    } catch (Exception e) {
                        log.error("Failed to send reminder email for request ID {}", request.getId(), e);
                    }
                }
            }
        }
    }
}