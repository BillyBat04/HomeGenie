package com.homegenie.userservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry registry;

    
    private Counter userRegisteredCounter;
    private Counter userLoginSuccessCounter;
    private Counter userLoginFailedCounter;
    private Counter userUpdatedCounter;

    
    private Timer registrationTimer;
    private Timer loginTimer;

    @PostConstruct
    public void init() {
        log.info("Initializing User Service custom metrics...");

        
        userRegisteredCounter = Counter.builder("user_registered_total")
                .description("Total number of users registered")
                .tag("service", "user-service")
                .tag("operation", "registration")
                .register(registry);

        registrationTimer = Timer.builder("user_registration_duration_seconds")
                .description("Time taken to register a new user")
                .tag("service", "user-service")
                .tag("operation", "registration")
                .register(registry);

        
        userLoginSuccessCounter = Counter.builder("user_login_total")
                .description("Total number of user login attempts")
                .tag("service", "user-service")
                .tag("operation", "login")
                .tag("status", "success")
                .register(registry);

        userLoginFailedCounter = Counter.builder("user_login_total")
                .description("Total number of user login attempts")
                .tag("service", "user-service")
                .tag("operation", "login")
                .tag("status", "failed")
                .register(registry);

        loginTimer = Timer.builder("user_login_duration_seconds")
                .description("Time taken to authenticate a user")
                .tag("service", "user-service")
                .tag("operation", "login")
                .register(registry);

        
        userUpdatedCounter = Counter.builder("user_updated_total")
                .description("Total number of user profile updates")
                .tag("service", "user-service")
                .tag("operation", "update")
                .register(registry);

        log.info("User Service custom metrics initialized successfully");
    }

    

    public void recordUserRegistration() {
        userRegisteredCounter.increment();
    }

    public void recordUserRegistrationTime(Runnable operation) {
        registrationTimer.record(operation);
    }

    public void recordLoginSuccess() {
        userLoginSuccessCounter.increment();
    }

    public void recordLoginFailed() {
        userLoginFailedCounter.increment();
    }

    public void recordLoginTime(Runnable operation) {
        loginTimer.record(operation);
    }

    public void recordUserUpdate() {
        userUpdatedCounter.increment();
    }
}
