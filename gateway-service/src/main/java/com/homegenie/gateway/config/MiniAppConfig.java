package com.homegenie.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Mini-App Configuration
 * 
 * Manages mini-app registry and routing configuration.
 * 
 * Configuration in application.yml:
 * ```yaml
 * mini-apps:
 *   apps:
 *     - id: maintenance
 *       name: Maintenance Request Management
 *       enabled: true
 *       routes:
 *         - /api/maintenance/**
 *     - id: marketplace
 *       name: Service Marketplace
 *       enabled: true
 *       routes:
 *         - /api/marketplace/**
 * ```
 * 
 * @version 1.0.0
 * @since Phase A4
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mini-apps")
public class MiniAppConfig {

    private List<MiniApp> apps = new ArrayList<>();

    /**
     * Mini-App Definition
     */
    @Data
    public static class MiniApp {
        /**
         * Unique mini-app identifier (e.g., "maintenance", "marketplace")
         */
        private String id;
        
        /**
         * Display name
         */
        private String name;
        
        /**
         * Whether mini-app is enabled
         */
        private boolean enabled = true;
        
        /**
         * Route patterns for this mini-app
         */
        private List<String> routes = new ArrayList<>();
        
        /**
         * Optional description
         */
        private String description;
    }

    /**
     * Get mini-app by ID
     */
    public MiniApp getMiniApp(String id) {
        return apps.stream()
                .filter(app -> app.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if mini-app is enabled
     */
    public boolean isEnabled(String id) {
        MiniApp app = getMiniApp(id);
        return app != null && app.isEnabled();
    }

    /**
     * Get all enabled mini-apps
     */
    public List<MiniApp> getEnabledApps() {
        return apps.stream()
                .filter(MiniApp::isEnabled)
                .toList();
    }
}
