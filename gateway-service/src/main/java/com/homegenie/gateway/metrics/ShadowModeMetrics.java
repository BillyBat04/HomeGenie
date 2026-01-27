package com.homegenie.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class ShadowModeMetrics {

    private final Counter shadowCallsTotal;
    private final Counter shadowCallsSuccess;
    private final Counter shadowCallsFailure;
    private final Counter shadowMismatchesTotal;
    private final Timer shadowCallDuration;
    
    private final AtomicLong currentRequests = new AtomicLong(0);

    public ShadowModeMetrics(MeterRegistry registry) {
        
        this.shadowCallsTotal = Counter.builder("shadow_calls_total")
            .description("Total number of shadow mode calls to Identity Service")
            .tag("service", "identity")
            .register(registry);

        
        this.shadowCallsSuccess = Counter.builder("shadow_calls_success_total")
            .description("Total successful shadow calls")
            .tag("service", "identity")
            .register(registry);

        
        this.shadowCallsFailure = Counter.builder("shadow_calls_failure_total")
            .description("Total failed shadow calls")
            .tag("service", "identity")
            .register(registry);

        
        this.shadowMismatchesTotal = Counter.builder("shadow_mismatches_total")
            .description("Total response mismatches between services")
            .tag("severity", "warning")
            .register(registry);

        
        this.shadowCallDuration = Timer.builder("shadow_call_duration_seconds")
            .description("Duration of shadow calls to Identity Service")
            .tag("service", "identity")
            .register(registry);
        
        
        registry.gauge("shadow_active_requests", currentRequests);
        
        log.info("Shadow Mode Metrics initialized");
    }

        public Timer.Sample startShadowCall() {
        shadowCallsTotal.increment();
        currentRequests.incrementAndGet();
        return Timer.start();
    }

        public void recordSuccess(Timer.Sample sample) {
        sample.stop(shadowCallDuration);
        shadowCallsSuccess.increment();
        currentRequests.decrementAndGet();
    }

        public void recordFailure(Timer.Sample sample) {
        sample.stop(shadowCallDuration);
        shadowCallsFailure.increment();
        currentRequests.decrementAndGet();
    }

        public void recordMismatch() {
        shadowMismatchesTotal.increment();
    }

        public MetricsSummary getSummary() {
        return new MetricsSummary(
            (long) shadowCallsTotal.count(),
            (long) shadowCallsSuccess.count(),
            (long) shadowCallsFailure.count(),
            (long) shadowMismatchesTotal.count(),
            currentRequests.get()
        );
    }

    public record MetricsSummary(
        long totalCalls,
        long successfulCalls,
        long failedCalls,
        long mismatches,
        long activeCalls
    ) {
        public double successRate() {
            return totalCalls > 0 ? (successfulCalls * 100.0 / totalCalls) : 0.0;
        }
        
        public double mismatchRate() {
            return successfulCalls > 0 ? (mismatches * 100.0 / successfulCalls) : 0.0;
        }
    }
}
