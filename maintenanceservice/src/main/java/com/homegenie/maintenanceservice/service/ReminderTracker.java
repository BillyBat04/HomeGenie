package com.homegenie.maintenanceservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class ReminderTracker {

    private final Map<String, Boolean> reminderCache = new ConcurrentHashMap<>();
    
    private LocalDate lastCleanupDate = LocalDate.now();

    public boolean isMaintenanceReminderSent(Long itemId) {
        cleanupIfNeeded();
        String key = buildMaintenanceKey(itemId);
        return reminderCache.containsKey(key);
    }

   
    public void markMaintenanceReminderSent(Long itemId) {
        String key = buildMaintenanceKey(itemId);
        reminderCache.put(key, true);
        log.debug("Marked maintenance reminder sent: {}", key);
    }

    public boolean isWarrantyReminderSent(Long itemId) {
        cleanupIfNeeded();
        String key = buildWarrantyKey(itemId);
        return reminderCache.containsKey(key);
    }

   
    public void markWarrantyReminderSent(Long itemId) {
        String key = buildWarrantyKey(itemId);
        reminderCache.put(key, true);
        log.debug("Marked warranty reminder sent: {}", key);
    }


    private String buildMaintenanceKey(Long itemId) {
        return String.format("maintenance:%d:%s", itemId, LocalDate.now());
    }


    private String buildWarrantyKey(Long itemId) {
        return String.format("warranty:%d:%s", itemId, LocalDate.now());
    }


    private void cleanupIfNeeded() {
        LocalDate today = LocalDate.now();
        
        if (!today.equals(lastCleanupDate)) {
            int oldSize = reminderCache.size();
            reminderCache.clear();
            lastCleanupDate = today;
            
            log.info("Reminder cache cleared (date changed): {} entries removed", oldSize);
        }
    }

    /**
     * Get current cache statistics
     */
    public Map<String, Object> getStatistics() {
        cleanupIfNeeded();
        
        long maintenanceCount = reminderCache.keySet().stream()
                .filter(k -> k.startsWith("maintenance:"))
                .count();
        
        long warrantyCount = reminderCache.keySet().stream()
                .filter(k -> k.startsWith("warranty:"))
                .count();
        
        return Map.of(
            "date", lastCleanupDate.toString(),
            "totalReminders", reminderCache.size(),
            "maintenanceReminders", maintenanceCount,
            "warrantyReminders", warrantyCount
        );
    }

    /**
     * Manual cleanup (for testing or admin operations)
     */
    public void clearCache() {
        int size = reminderCache.size();
        reminderCache.clear();
        log.info("Reminder cache manually cleared: {} entries removed", size);
    }
}
