package com.homegenie.gateway.monitor;

import com.homegenie.gateway.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "platform.auto-rollback.enabled", havingValue = "true", matchIfMissing = true)
public class AutoRollbackMonitor {

    @Value("${platform.auto-rollback.error-rate-threshold:1.0}")
    private double errorRateThreshold;

    @Value("${platform.auto-rollback.check-interval-seconds:60}")
    private int checkIntervalSeconds;

    @Value("${platform.auto-rollback.min-requests:100}")
    private int minRequests;

    @Value("${platform.auto-rollback.response-time-threshold:200}")
    private long responseTimeThreshold;

    @Value("${platform.traffic-split.percentage:0}")
    private int currentTrafficPercentage;

    @Autowired(required = false)
    private AlertService alertService;

    
    private final AtomicLong userServiceRequests = new AtomicLong(0);
    private final AtomicLong userServiceErrors = new AtomicLong(0);
    private final List<Long> userServiceResponseTimes = new ArrayList<>();

    
    private final AtomicLong identityServiceRequests = new AtomicLong(0);
    private final AtomicLong identityServiceErrors = new AtomicLong(0);
    private final List<Long> identityServiceResponseTimes = new ArrayList<>();

    
    private boolean rolledBack = false;
    private int consecutiveErrorIntervals = 0;
    private static final int MAX_CONSECUTIVE_ERRORS = 3;

        public void recordUserServiceRequest(boolean success, long responseTimeMs) {
        userServiceRequests.incrementAndGet();
        if (!success) {
            userServiceErrors.incrementAndGet();
        }
        synchronized (userServiceResponseTimes) {
            userServiceResponseTimes.add(responseTimeMs);
        }
    }

        public void recordUserServiceRequest(boolean success) {
        recordUserServiceRequest(success, 0);
    }

        public void recordIdentityServiceRequest(boolean success, long responseTimeMs) {
        identityServiceRequests.incrementAndGet();
        if (!success) {
            identityServiceErrors.incrementAndGet();
        }
        synchronized (identityServiceResponseTimes) {
            identityServiceResponseTimes.add(responseTimeMs);
        }
    }

        public void recordIdentityServiceRequest(boolean success) {
        recordIdentityServiceRequest(success, 0);
    }

        @Scheduled(fixedDelayString = "${platform.auto-rollback.check-interval-seconds:60}000")
    public void checkErrorRates() {
        long userReqs = userServiceRequests.get();
        long userErrs = userServiceErrors.get();
        long identityReqs = identityServiceRequests.get();
        long identityErrs = identityServiceErrors.get();

        
        if (identityReqs < minRequests) {
            log.debug("Not enough Identity Service requests ({}/{}), skipping check", identityReqs, minRequests);
            return;
        }

        
        double userErrorRate = userReqs > 0 ? (double) userErrs / userReqs * 100 : 0;
        double identityErrorRate = (double) identityErrs / identityReqs * 100;

        
        long userP95 = calculateP95ResponseTime(userServiceResponseTimes);
        long identityP95 = calculateP95ResponseTime(identityServiceResponseTimes);

        log.info("Health Check:");
        log.info("   User Service: {}/{} requests, {:.2f}% error rate, p95={}ms", 
            userReqs, userErrs, userErrorRate, userP95);
        log.info("   Identity Service: {}/{} requests, {:.2f}% error rate, p95={}ms", 
            identityReqs, identityErrs, identityErrorRate, identityP95);
        log.info("   Traffic Split: {}%, Consecutive Errors: {}", 
            currentTrafficPercentage, consecutiveErrorIntervals);

        
        boolean shouldRollback = false;
        String rollbackReason = "";
        String alertLevel = "INFO";

        
        if (identityErrorRate > errorRateThreshold) {
            shouldRollback = true;
            rollbackReason = String.format("Identity Service error rate %.2f%% exceeds threshold %.2f%%", 
                identityErrorRate, errorRateThreshold);
            alertLevel = "CRITICAL";
            consecutiveErrorIntervals++;
        }

        
        else if (identityErrorRate > userErrorRate * 2 && identityErrorRate > 0.5) {
            shouldRollback = true;
            rollbackReason = String.format("Identity Service error rate %.2f%% is >2x User Service rate %.2f%%", 
                identityErrorRate, userErrorRate);
            alertLevel = "CRITICAL";
            consecutiveErrorIntervals++;
        }

        
        else if (identityP95 > responseTimeThreshold && identityP95 > userP95 * 1.5) {
            shouldRollback = true;
            rollbackReason = String.format("Identity Service p95 response time %dms exceeds threshold %dms (User Service: %dms)", 
                identityP95, responseTimeThreshold, userP95);
            alertLevel = "WARNING";
            consecutiveErrorIntervals++;
        }

        
        else if (consecutiveErrorIntervals >= MAX_CONSECUTIVE_ERRORS) {
            shouldRollback = true;
            rollbackReason = String.format("Consecutive error intervals (%d) exceeded threshold (%d)", 
                consecutiveErrorIntervals, MAX_CONSECUTIVE_ERRORS);
            alertLevel = "CRITICAL";
        }

        
        else {
            consecutiveErrorIntervals = 0;
        }

        
        if (shouldRollback && !rolledBack) {
            performRollback(rollbackReason, alertLevel);
        } else if (!shouldRollback && rolledBack) {
            log.info("Health metrics normalized, ready to re-enable traffic split manually");
            if (alertService != null) {
                alertService.info("Auto-Rollback Recovery", 
                    "System health restored. Manual intervention required to re-enable traffic split.");
            }
            rolledBack = false;
            consecutiveErrorIntervals = 0;
        }

        
        resetMetrics();
    }

        private void performRollback(String reason, String alertLevel) {
        log.error("🚨 AUTO-ROLLBACK TRIGGERED: {}", reason);
        
        
        int newPercentage;
        if (currentTrafficPercentage >= 50) {
            newPercentage = 50;
            log.warn("🔙 Gradual rollback: Reducing traffic to 50%");
        } else if (currentTrafficPercentage > 0) {
            newPercentage = 0;
            log.error("🔙 Full rollback: Routing 100% traffic to User Service");
        } else {
            log.info("Already rolled back to 0%");
            return;
        }

        currentTrafficPercentage = newPercentage;
        rolledBack = true;

        
        String fullMessage = String.format(
            "Auto-Rollback Triggered\n" +
            "Reason: %s\n" +
            "Traffic reduced: %d%%\n" +
            "Action Required: Manual investigation needed before re-enabling traffic split",
            reason, newPercentage
        );

        if (alertService != null) {
            if ("CRITICAL".equals(alertLevel)) {
                alertService.critical("AUTO-ROLLBACK TRIGGERED", fullMessage);
            } else {
                alertService.warning("AUTO-ROLLBACK TRIGGERED", fullMessage);
            }
        } else {
            log.error("🚨 ALERT: AUTO-ROLLBACK TRIGGERED - {}", fullMessage);
        }
    }

        private long calculateP95ResponseTime(List<Long> responseTimes) {
        if (responseTimes.isEmpty()) {
            return 0;
        }

        List<Long> sortedTimes;
        synchronized (responseTimes) {
            sortedTimes = new ArrayList<>(responseTimes);
        }
        
        if (sortedTimes.isEmpty()) {
            return 0;
        }

        sortedTimes.sort(Long::compareTo);
        int p95Index = (int) Math.ceil(sortedTimes.size() * 0.95) - 1;
        return sortedTimes.get(Math.max(0, p95Index));
    }

        private void resetMetrics() {
        userServiceRequests.set(0);
        userServiceErrors.set(0);
        identityServiceRequests.set(0);
        identityServiceErrors.set(0);
        
        synchronized (userServiceResponseTimes) {
            userServiceResponseTimes.clear();
        }
        synchronized (identityServiceResponseTimes) {
            identityServiceResponseTimes.clear();
        }
    }

        public String getMetrics() {
        return String.format(
            "UserService[reqs=%d, errs=%d] IdentityService[reqs=%d, errs=%d] TrafficSplit=%d%% RolledBack=%s",
            userServiceRequests.get(),
            userServiceErrors.get(),
            identityServiceRequests.get(),
            identityServiceErrors.get(),
            currentTrafficPercentage,
            rolledBack
        );
    }
}
