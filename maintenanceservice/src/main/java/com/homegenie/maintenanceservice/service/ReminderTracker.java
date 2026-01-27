package com.homegenie.maintenanceservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track and prevent duplicate reminder notifications
 * Uses in-memory cache with daily key to ensure reminders sent once per day
 * 
 * Cache automatically cleared at midnight via scheduled cleanup
 */
@Service
@Slf4j
public class ReminderTracker {

    // Key format: "maintenance:{itemId}:{date}" or "warranty:{itemId}:{date}"
    private final Map<String, Boolean> reminderCache = new ConcurrentHashMap<>();
    
    private LocalDate lastCleanupDate = LocalDate.now();

    /**
     * Check if maintenance reminder already sent today
     * @return true if already sent, false if can send
     */
    public boolean isMaintenanceReminderSent(Long itemId) {
        cleanupIfNeeded();
        String key = buildMaintenanceKey(itemId);
        return reminderCache.containsKey(key);
    }

    /**
     * Mark maintenance reminder as sent for today
     */
    public void markMaintenanceReminderSent(Long itemId) {
        String key = buildMaintenanceKey(itemId);
        reminderCache.put(key, true);
        log.debug("📝 Marked maintenance reminder sent: {}", key);
    }

    /**
     * Check if warranty reminder already sent today
     * @return true if already sent, false if can send
     */
    public boolean isWarrantyReminderSent(Long itemId) {
        cleanupIfNeeded();
        String key = buildWarrantyKey(itemId);
        return reminderCache.containsKey(key);
    }

    /**
     * Mark warranty reminder as sent for today
     */
    public void markWarrantyReminderSent(Long itemId) {
        String key = buildWarrantyKey(itemId);
        reminderCache.put(key, true);
        log.debug("📝 Marked warranty reminder sent: {}", key);
    }

    /**
     * Build cache key for maintenance reminder
     */
    private String buildMaintenanceKey(Long itemId) {
        return String.format("maintenance:%d:%s", itemId, LocalDate.now());
    }

    /**
     * Build cache key for warranty reminder
     */
    private String buildWarrantyKey(Long itemId) {
        return String.format("warranty:%d:%s", itemId, LocalDate.now());
    }

    /**
     * Cleanup old cache entries when date changes
     * Automatically called before each check
     */
    private void cleanupIfNeeded() {
        LocalDate today = LocalDate.now();
        
        if (!today.equals(lastCleanupDate)) {
            int oldSize = reminderCache.size();
            reminderCache.clear();
            lastCleanupDate = today;
            
            log.info("🧹 Reminder cache cleared (date changed): {} entries removed", oldSize);
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
        log.info("🧹 Reminder cache manually cleared: {} entries removed", size);
    }
}
