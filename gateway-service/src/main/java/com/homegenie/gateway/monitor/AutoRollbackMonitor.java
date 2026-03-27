package com.homegenie.gateway.monitor;

import com.homegenie.gateway.config.TrafficSplitState;
import com.homegenie.gateway.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "platform.auto-rollback.enabled", havingValue = "true", matchIfMissing = true)
public class AutoRollbackMonitor {

    private static final int SLIDING_WINDOW_SIZE = 1000;

    @Value("${platform.auto-rollback.error-rate-threshold:1.0}")
    private double errorRateThreshold;

    @Value("${platform.auto-rollback.check-interval-seconds:60}")
    private int checkIntervalSeconds;

    @Value("${platform.auto-rollback.min-requests:100}")
    private int minRequests;

    @Value("${platform.auto-rollback.response-time-threshold:200}")
    private long responseTimeThreshold;

    private final TrafficSplitState trafficSplitState;

    @Autowired(required = false)
    private AlertService alertService;

    private final AtomicLong userServiceRequests = new AtomicLong(0);
    private final AtomicLong userServiceErrors = new AtomicLong(0);
    private final Deque<Long> userServiceResponseTimes = new ArrayDeque<>();

    private final AtomicLong identityServiceRequests = new AtomicLong(0);
    private final AtomicLong identityServiceErrors = new AtomicLong(0);
    private final Deque<Long> identityServiceResponseTimes = new ArrayDeque<>();

    public AutoRollbackMonitor(TrafficSplitState trafficSplitState) {
        this.trafficSplitState = trafficSplitState;
    }

    
    private boolean rolledBack = false;
    private int consecutiveErrorIntervals = 0;
    private static final int MAX_CONSECUTIVE_ERRORS = 3;

    public void recordUserServiceRequest(boolean success, long responseTimeMs) {
        userServiceRequests.incrementAndGet();
        if (!success) {
            userServiceErrors.incrementAndGet();
        }
        synchronized (userServiceResponseTimes) {
            if (userServiceResponseTimes.size() >= SLIDING_WINDOW_SIZE) {
                userServiceResponseTimes.pollFirst();
            }
            userServiceResponseTimes.addLast(responseTimeMs);
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
            if (identityServiceResponseTimes.size() >= SLIDING_WINDOW_SIZE) {
                identityServiceResponseTimes.pollFirst();
            }
            identityServiceResponseTimes.addLast(responseTimeMs);
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
        log.info("   User Service: {}/{} requests, {}% error rate, p95={}ms",
            userReqs, userErrs, String.format("%.2f", userErrorRate), userP95);
        log.info("   Identity Service: {}/{} requests, {}% error rate, p95={}ms",
            identityReqs, identityErrs, String.format("%.2f", identityErrorRate), identityP95);
        log.info("   Traffic Split: {}%, Consecutive Errors: {}",
            trafficSplitState.getTrafficPercentage(), consecutiveErrorIntervals);

        
        boolean shouldRollback = false;
        String rollbackReason = "";
        String alertLevel = "INFO";

        
        if (identityErrorRate > errorRateThreshold) {
            shouldRollback = true;
            rollbackReason = String.format("Identity Service error rate %.2f%% exceeds threshold %.2f%%",
                identityErrorRate, errorRateThreshold);
            alertLevel = "CRITICAL";
            consecutiveErrorIntervals++;
        } else if (identityErrorRate > userErrorRate * 2 && identityErrorRate > 0.5) {
            shouldRollback = true;
            rollbackReason = String.format("Identity Service error rate %.2f%% is >2x User Service rate %.2f%%",
                identityErrorRate, userErrorRate);
            alertLevel = "CRITICAL";
            consecutiveErrorIntervals++;
        } else if (identityP95 > responseTimeThreshold && identityP95 > userP95 * 1.5) {
            shouldRollback = true;
            rollbackReason = String.format("Identity Service p95 %dms exceeds threshold %dms (User Service: %dms)",
                identityP95, responseTimeThreshold, userP95);
            alertLevel = "WARNING";
            consecutiveErrorIntervals++;
        } else if (consecutiveErrorIntervals >= MAX_CONSECUTIVE_ERRORS) {
            shouldRollback = true;
            rollbackReason = String.format("Consecutive error intervals (%d) exceeded threshold (%d)",
                consecutiveErrorIntervals, MAX_CONSECUTIVE_ERRORS);
            alertLevel = "CRITICAL";
        } else {
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
        log.error("AUTO-ROLLBACK TRIGGERED: {}", reason);

        int current = trafficSplitState.getTrafficPercentage();
        int newPercentage;
        if (current >= 50) {
            newPercentage = 50;
            log.warn("Gradual rollback: Reducing traffic to 50%");
        } else if (current > 0) {
            newPercentage = 0;
            log.error("Full rollback: Routing 100% traffic to User Service");
        } else {
            log.info("Already rolled back to 0%");
            return;
        }

        trafficSplitState.setTrafficPercentage(newPercentage);
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

    private long calculateP95ResponseTime(Deque<Long> responseTimes) {
        List<Long> sortedTimes;
        synchronized (responseTimes) {
            if (responseTimes.isEmpty()) return 0;
            sortedTimes = new ArrayList<>(responseTimes);
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
            trafficSplitState.getTrafficPercentage(),
            rolledBack
        );
    }
}
