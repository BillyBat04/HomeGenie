package com.homegenie.marketplaceservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI marketplaceServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Marketplace Service API")
                        .description("External service provider booking platform for HomeGenie Super App. " +
                                     "This mini-app allows users to book plumbers, electricians, cleaners, " +
                                     "and other service providers when internal maintenance team is not available.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HomeGenie Team")
                                .email("support@homegenie.com")
                                .url("https://homegenie.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8085")
                                .description("Local Development"),
                        new Server()
                                .url("https://api.homegenie.com/marketplace")
                                .description("Production")
                ));
    }
}
