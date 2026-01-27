package com.homegenie.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Configuration for User Service
 * Following Lean Swagger principles - API contract only, no business logic docs
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "User Service API",
        version = "v1",
        description = "Authentication and user management operations"
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {
}
