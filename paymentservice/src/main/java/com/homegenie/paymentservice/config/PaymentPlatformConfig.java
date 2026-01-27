package com.homegenie.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Payment Platform Configuration
 * Manages commission rates and settings for all mini-apps
 */
@Configuration
@ConfigurationProperties(prefix = "payment-platform")
@Data
public class PaymentPlatformConfig {

    private Map<String, MiniAppConfig> miniApps = new HashMap<>();

    @Data
    public static class MiniAppConfig {
        private BigDecimal commissionRate;
        private boolean enabled;
        private String displayName;
        private String description;
    }

    /**
     * Get commission rate for a specific mini-app
     */
    public BigDecimal getCommissionRate(String miniAppId) {
        MiniAppConfig config = miniApps.get(miniAppId);
        if (config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Mini-app not found or disabled: " + miniAppId);
        }
        return config.getCommissionRate();
    }

    /**
     * Check if mini-app is enabled
     */
    public boolean isEnabled(String miniAppId) {
        MiniAppConfig config = miniApps.get(miniAppId);
        return config != null && config.isEnabled();
    }

    /**
     * Validate mini-app exists and is enabled
     */
    public void validateMiniApp(String miniAppId) {
        if (!isEnabled(miniAppId)) {
            throw new IllegalArgumentException("Mini-app not found or disabled: " + miniAppId);
        }
    }
}
