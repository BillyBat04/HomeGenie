package com.homegenie.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TrafficSplitState {

    private final AtomicInteger trafficPercentage;
    private final AtomicBoolean enabled;

    public TrafficSplitState(
            @Value("${platform.traffic-split.percentage:0}") int initialPercentage,
            @Value("${platform.traffic-split.enabled:false}") boolean initialEnabled) {
        this.trafficPercentage = new AtomicInteger(Math.max(0, Math.min(100, initialPercentage)));
        this.enabled = new AtomicBoolean(initialEnabled);
    }

    public int getTrafficPercentage() {
        return trafficPercentage.get();
    }

    public void setTrafficPercentage(int percentage) {
        trafficPercentage.set(Math.max(0, Math.min(100, percentage)));
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean isEnabled) {
        this.enabled.set(isEnabled);
    }
}
