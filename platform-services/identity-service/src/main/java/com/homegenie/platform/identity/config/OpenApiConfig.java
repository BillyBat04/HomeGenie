package com.homegenie.platform.identity.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 * API documentation for Identity Platform Service
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI identityPlatformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Identity Platform Service API")
                        .description("Platform-level Identity and Authentication Service for HomeGenie Super App")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HomeGenie Platform Team")
                                .email("platform@homegenie.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8086")
                                .description("Identity Service (Development)"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway (Development)")
                ));
    }
}
