package com.homegenie.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


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

    
    public BigDecimal getCommissionRate(String miniAppId) {
        MiniAppConfig config = miniApps.get(miniAppId);
        if (config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Mini-app not found or disabled: " + miniAppId);
        }
        return config.getCommissionRate();
    }

    
    public boolean isEnabled(String miniAppId) {
        MiniAppConfig config = miniApps.get(miniAppId);
        return config != null && config.isEnabled();
    }

    
    public void validateMiniApp(String miniAppId) {
        if (!isEnabled(miniAppId)) {
            throw new IllegalArgumentException("Mini-app not found or disabled: " + miniAppId);
        }
    }
}
