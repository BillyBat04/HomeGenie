package com.homegenie.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Data
@Configuration
@ConfigurationProperties(prefix = "mini-apps")
public class MiniAppConfig {

    private List<MiniApp> apps = new ArrayList<>();

    
    @Data
    public static class MiniApp {
        
        private String id;
        
        
        private String name;
        
        
        private boolean enabled = true;
        
        
        private List<String> routes = new ArrayList<>();
        
        
        private String description;
    }

    
    public MiniApp getMiniApp(String id) {
        return apps.stream()
                .filter(app -> app.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    
    public boolean isEnabled(String id) {
        MiniApp app = getMiniApp(id);
        return app != null && app.isEnabled();
    }

    
    public List<MiniApp> getEnabledApps() {
        return apps.stream()
                .filter(MiniApp::isEnabled)
                .toList();
    }
}
