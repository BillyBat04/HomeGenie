package com.homegenie.maintenanceservice.scheduler;

import com.homegenie.maintenanceservice.dto.event.MaintenanceReminderEvent;
import com.homegenie.maintenanceservice.dto.event.WarrantyExpiringEvent;
import com.homegenie.maintenanceservice.model.Item;
import com.homegenie.maintenanceservice.repository.ItemRepository;
import com.homegenie.maintenanceservice.service.MaintenanceEventPublisher;
import com.homegenie.maintenanceservice.service.ReminderTracker;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ItemMaintenanceScheduler {

    private final ItemRepository itemRepository;
    private final MaintenanceEventPublisher eventPublisher;
    private final ReminderTracker reminderTracker;
    private final MeterRegistry meterRegistry;

    
    private final AtomicInteger maintenanceFailureCount = new AtomicInteger(0);
    private final AtomicInteger warrantyFailureCount = new AtomicInteger(0);
    
    @Value("${scheduler.alert.failure-threshold:3}")
    private int failureThreshold;
    
    @Value("${scheduler.maintenance-reminder.enabled:true}")
    private boolean maintenanceReminderEnabled;
    
    @Value("${scheduler.warranty-reminder.enabled:true}")
    private boolean warrantyReminderEnabled;

        @Scheduled(cron = "${scheduler.maintenance-reminder.cron:0 0 8 * * *}")
    @Timed(value = "scheduler.maintenance.reminder", description = "Time taken to check maintenance reminders")
    @Counted(value = "scheduler.maintenance.reminder.executions", description = "Number of maintenance reminder checks")
    // Note: @ConditionalOnProperty does NOT work on methods — class-level annotation above already controls this.
    // The maintenanceReminderEnabled flag check inside the method body handles per-run toggling.
    public void checkMaintenanceReminders() {
        if (!maintenanceReminderEnabled) {
            log.debug(" Maintenance reminder scheduler disabled via feature flag");
            return;
        }
        
        log.info("🕐 Starting maintenance reminder check at {}", Instant.now());
        
        try {
            LocalDate today = LocalDate.now();
            LocalDate sevenDaysFromNow = today.plusDays(7);
            
            
            List<Item> itemsDueSoon = itemRepository.findItemsMaintenanceDueSoon(today, sevenDaysFromNow);
            
            log.info("Found {} items needing maintenance attention", itemsDueSoon.size());
            
            int overdueCount = 0;
            int dueNowCount = 0;
            int dueSoonCount = 0;
            int skippedDuplicates = 0;
            int publishedCount = 0;
            
            for (Item item : itemsDueSoon) {
                try {
                    
                    if (reminderTracker.isMaintenanceReminderSent(item.getId())) {
                        skippedDuplicates++;
                        log.debug(" Skipping duplicate reminder for item {}", item.getId());
                        continue;
                    }
                    
                    String urgencyLevel = determineMaintenanceUrgency(item, today);
                    
                    switch (urgencyLevel) {
                        case "OVERDUE": overdueCount++; break;
                        case "DUE_NOW": dueNowCount++; break;
                        case "DUE_SOON": dueSoonCount++; break;
                    }
                    
                    publishMaintenanceReminder(item, urgencyLevel);
                    reminderTracker.markMaintenanceReminderSent(item.getId());
                    publishedCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to process maintenance reminder for item {}: {}", 
                            item.getId(), e.getMessage());
                    meterRegistry.counter("scheduler.maintenance.reminder.item.errors").increment();
                }
            }
            
            
            meterRegistry.counter("scheduler.maintenance.reminder.total", "type", "overdue").increment(overdueCount);
            meterRegistry.counter("scheduler.maintenance.reminder.total", "type", "due_now").increment(dueNowCount);
            meterRegistry.counter("scheduler.maintenance.reminder.total", "type", "due_soon").increment(dueSoonCount);
            meterRegistry.counter("scheduler.maintenance.reminder.duplicates_skipped").increment(skippedDuplicates);
            
            log.info("Maintenance reminder check complete: {} overdue, {} due now, {} due soon, {} published, {} skipped duplicates", 
                    overdueCount, dueNowCount, dueSoonCount, publishedCount, skippedDuplicates);
            
            
            maintenanceFailureCount.set(0);
            meterRegistry.counter("scheduler.maintenance.reminder.success").increment();
                    
        } catch (Exception e) {
            log.error("Failed to execute maintenance reminder check: {}", e.getMessage(), e);
            handleMaintenanceFailure(e);
        }
    }

        @Scheduled(cron = "${scheduler.warranty-reminder.cron:0 15 8 * * *}")
    @Timed(value = "scheduler.warranty.reminder", description = "Time taken to check warranty expirations")
    @Counted(value = "scheduler.warranty.reminder.executions", description = "Number of warranty reminder checks")
    // Note: @ConditionalOnProperty does NOT work on methods — class-level annotation above already controls this.
    public void checkWarrantyExpirations() {
        if (!warrantyReminderEnabled) {
            log.debug("Warranty reminder scheduler disabled via feature flag");
            return;
        }
        
        log.info(" Starting warranty expiration check at {}", Instant.now());
        
        try {
            LocalDate today = LocalDate.now();
            LocalDate thirtyDaysFromNow = today.plusDays(30);
            
            
            List<Item> itemsWithWarrantyExpiring = 
                    itemRepository.findItemsWithWarrantyExpiringSoon(today, thirtyDaysFromNow);
            
            log.info("Found {} items with warranty expiring soon", itemsWithWarrantyExpiring.size());
            
            int expiredCount = 0;
            int expiring1DayCount = 0;
            int expiring7DaysCount = 0;
            int expiring30DaysCount = 0;
            int skippedDuplicates = 0;
            int publishedCount = 0;
            
            for (Item item : itemsWithWarrantyExpiring) {
                try {
                    
                    if (reminderTracker.isWarrantyReminderSent(item.getId())) {
                        skippedDuplicates++;
                        log.debug("⏭Skipping duplicate warranty reminder for item {}", item.getId());
                        continue;
                    }
                    
                    String urgencyLevel = determineWarrantyUrgency(item, today);
                    
                    switch (urgencyLevel) {
                        case "EXPIRED": expiredCount++; break;
                        case "EXPIRING_SOON_1DAY": expiring1DayCount++; break;
                        case "EXPIRING_SOON_7DAYS": expiring7DaysCount++; break;
                        case "EXPIRING_SOON_30DAYS": expiring30DaysCount++; break;
                    }
                    
                    publishWarrantyExpiringAlert(item, urgencyLevel);
                    reminderTracker.markWarrantyReminderSent(item.getId());
                    publishedCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to process warranty reminder for item {}: {}", 
                            item.getId(), e.getMessage());
                    meterRegistry.counter("scheduler.warranty.reminder.item.errors").increment();
                }
            }
            
            
            meterRegistry.counter("scheduler.warranty.reminder.total", "type", "expired").increment(expiredCount);
            meterRegistry.counter("scheduler.warranty.reminder.total", "type", "expiring_1day").increment(expiring1DayCount);
            meterRegistry.counter("scheduler.warranty.reminder.total", "type", "expiring_7days").increment(expiring7DaysCount);
            meterRegistry.counter("scheduler.warranty.reminder.total", "type", "expiring_30days").increment(expiring30DaysCount);
            meterRegistry.counter("scheduler.warranty.reminder.duplicates_skipped").increment(skippedDuplicates);
            
            log.info("Warranty expiration check complete: {} expired, {} expiring in 1 day, {} in 7 days, {} in 30 days, {} published, {} skipped duplicates", 
                    expiredCount, expiring1DayCount, expiring7DaysCount, expiring30DaysCount, publishedCount, skippedDuplicates);
            
            
            warrantyFailureCount.set(0);
            meterRegistry.counter("scheduler.warranty.reminder.success").increment();
                    
        } catch (Exception e) {
            log.error("Failed to execute warranty expiration check: {}", e.getMessage(), e);
            handleWarrantyFailure(e);
        }
    }

        private String determineMaintenanceUrgency(Item item, LocalDate today) {
        LocalDate nextMaintenanceDate = item.getNextMaintenanceDate();
        
        if (nextMaintenanceDate == null) {
            return "DUE_SOON"; 
        }
        
        if (nextMaintenanceDate.isBefore(today)) {
            return "OVERDUE";
        } else if (nextMaintenanceDate.isEqual(today)) {
            return "DUE_NOW";
        } else {
            return "DUE_SOON"; 
        }
    }

        private String determineWarrantyUrgency(Item item, LocalDate today) {
        LocalDate warrantyExpiryDate = item.getWarrantyExpiryDate();
        
        if (warrantyExpiryDate == null) {
            return "EXPIRING_SOON_30DAYS"; 
        }
        
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, warrantyExpiryDate);
        
        if (daysUntilExpiry < 0) {
            return "EXPIRED";
        } else if (daysUntilExpiry <= 1) {
            return "EXPIRING_SOON_1DAY";
        } else if (daysUntilExpiry <= 7) {
            return "EXPIRING_SOON_7DAYS";
        } else {
            return "EXPIRING_SOON_30DAYS";
        }
    }

        private void publishMaintenanceReminder(Item item, String urgencyLevel) {
        int daysUntilMaintenance = item.getNextMaintenanceDate() != null 
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(), item.getNextMaintenanceDate())
                : 0;
        
        MaintenanceReminderEvent event = MaintenanceReminderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .itemId(item.getId())
                .itemName(item.getName())
                .itemCategory(item.getCategory().toString())
                .itemLocation(item.getLocation())
                .userId(item.getUserId())
                .userName(null) 
                .userEmail(null) 
                .nextMaintenanceDate(item.getNextMaintenanceDate())
                .daysUntilMaintenance(daysUntilMaintenance)
                .lastMaintenanceDate(item.getLastMaintenanceDate())
                .maintenanceFrequencyDays(item.getMaintenanceFrequencyDays())
                .urgencyLevel(urgencyLevel)
                .eventType("MAINTENANCE_REMINDER")
                .timestamp(Instant.now())
                .build();
        
        eventPublisher.publishMaintenanceReminderEvent(event);
        
        log.debug("Published maintenance reminder: itemId={}, urgency={}", 
                item.getId(), urgencyLevel);
    }

        private void publishWarrantyExpiringAlert(Item item, String urgencyLevel) {
        int daysUntilExpiry = item.getWarrantyExpiryDate() != null
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(), item.getWarrantyExpiryDate())
                : 0;
        
        WarrantyExpiringEvent event = WarrantyExpiringEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .itemId(item.getId())
                .itemName(item.getName())
                .itemCategory(item.getCategory().toString())
                .itemBrand(item.getBrand())
                .itemModel(item.getModel())
                .userId(item.getUserId())
                .userName(null) 
                .userEmail(null) 
                .warrantyExpiryDate(item.getWarrantyExpiryDate())
                .daysUntilExpiry(daysUntilExpiry)
                .purchaseDate(item.getPurchaseDate())
                .warrantyDocumentUrl(item.getWarrantyDocumentUrl())
                .urgencyLevel(urgencyLevel)
                .eventType("WARRANTY_EXPIRING")
                .timestamp(Instant.now())
                .build();
        
        eventPublisher.publishWarrantyExpiringEvent(event);
        
        log.debug("Published warranty expiring alert: itemId={}, urgency={}", 
                item.getId(), urgencyLevel);
    }

    

        private void handleMaintenanceFailure(Exception e) {
        int failures = maintenanceFailureCount.incrementAndGet();
        meterRegistry.counter("scheduler.maintenance.reminder.failures").increment();
        meterRegistry.gauge("scheduler.maintenance.reminder.consecutive_failures", maintenanceFailureCount);
        
        if (failures >= failureThreshold) {
            log.error("ALERT: Maintenance reminder scheduler failed {} times consecutively! Threshold: {}", 
                    failures, failureThreshold);
            log.error(" Last error: {}", e.getMessage(), e);
            
            
            
        } else {
            log.warn("Maintenance reminder scheduler failed ({}/{}): {}", 
                    failures, failureThreshold, e.getMessage());
        }
    }

        private void handleWarrantyFailure(Exception e) {
        int failures = warrantyFailureCount.incrementAndGet();
        meterRegistry.counter("scheduler.warranty.reminder.failures").increment();
        meterRegistry.gauge("scheduler.warranty.reminder.consecutive_failures", warrantyFailureCount);
        
        if (failures >= failureThreshold) {
            log.error("ALERT: Warranty reminder scheduler failed {} times consecutively! Threshold: {}", 
                    failures, failureThreshold);
            log.error("Last error: {}", e.getMessage(), e);
            
            
            
        } else {
            log.warn("Warranty reminder scheduler failed ({}/{}): {}", 
                    failures, failureThreshold, e.getMessage());
        }
    }
}
