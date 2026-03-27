package com.homegenie.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class PublicRoutesConfig {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private List<String> publicRoutes = new ArrayList<>();

    @SuppressWarnings("null")
    public boolean isPublicRoute(String path) {
        return publicRoutes.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
