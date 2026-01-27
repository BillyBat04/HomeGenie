package com.homegenie.maintenanceservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry registry;

    
    private Counter requestCreatedCounter;
    private Counter requestAssignedCounter;
    private Counter requestCompletedCounter;
    private Counter requestCancelledCounter;

    
    private Timer requestProcessingTimer;
    private Timer assignmentTimer;

    
    private AtomicInteger pendingRequests = new AtomicInteger(0);
    private AtomicInteger inProgressRequests = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        log.info("Initializing Maintenance Service custom metrics...");

        
        requestCreatedCounter = Counter.builder("maintenance_request_created_total")
                .description("Total number of maintenance requests created")
                .tag("service", "maintenance-service")
                .tag("operation", "create")
                .register(registry);

        
        requestAssignedCounter = Counter.builder("maintenance_request_assigned_total")
                .description("Total number of maintenance requests assigned")
                .tag("service", "maintenance-service")
                .tag("operation", "assign")
                .register(registry);

        assignmentTimer = Timer.builder("maintenance_assignment_duration_seconds")
                .description("Time taken to assign a technician")
                .tag("service", "maintenance-service")
                .tag("operation", "assign")
                .register(registry);

        
        requestCompletedCounter = Counter.builder("maintenance_request_completed_total")
                .description("Total number of maintenance requests completed")
                .tag("service", "maintenance-service")
                .tag("operation", "complete")
                .register(registry);

        requestProcessingTimer = Timer.builder("maintenance_processing_duration_seconds")
                .description("Time from creation to completion")
                .tag("service", "maintenance-service")
                .tag("operation", "process")
                .register(registry);

        
        requestCancelledCounter = Counter.builder("maintenance_request_cancelled_total")
                .description("Total number of maintenance requests cancelled")
                .tag("service", "maintenance-service")
                .tag("operation", "cancel")
                .register(registry);

        
        Gauge.builder("maintenance_requests_pending", pendingRequests, AtomicInteger::get)
                .description("Current number of pending maintenance requests")
                .tag("service", "maintenance-service")
                .tag("status", "pending")
                .register(registry);

        
        Gauge.builder("maintenance_requests_in_progress", inProgressRequests, AtomicInteger::get)
                .description("Current number of in-progress maintenance requests")
                .tag("service", "maintenance-service")
                .tag("status", "in_progress")
                .register(registry);

        log.info("Maintenance Service custom metrics initialized successfully");
    }

    

    public void recordRequestCreated() {
        requestCreatedCounter.increment();
        pendingRequests.incrementAndGet();
    }

    public void recordRequestAssigned() {
        requestAssignedCounter.increment();
        pendingRequests.decrementAndGet();
        inProgressRequests.incrementAndGet();
    }

    public void recordAssignmentTime(Runnable operation) {
        assignmentTimer.record(operation);
    }

    public void recordRequestCompleted() {
        requestCompletedCounter.increment();
        inProgressRequests.decrementAndGet();
    }

    public void recordRequestProcessingTime(Runnable operation) {
        requestProcessingTimer.record(operation);
    }

    public void recordRequestCancelled(String currentStatus) {
        requestCancelledCounter.increment();
        if ("PENDING".equals(currentStatus)) {
            pendingRequests.decrementAndGet();
        } else if ("IN_PROGRESS".equals(currentStatus)) {
            inProgressRequests.decrementAndGet();
        }
    }

    
    public void updatePendingCount(int count) {
        pendingRequests.set(count);
    }

    public void updateInProgressCount(int count) {
        inProgressRequests.set(count);
    }
}
