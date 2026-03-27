package com.homegenie.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping(value = "/fallback/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> fallback(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        String path = exchange.getRequest().getURI().getPath();
        String service = path.replace("/fallback/", "");
        return Mono.just(Map.of(
            "status", 503,
            "error", "Service Unavailable",
            "message", "The " + service + " is temporarily unavailable. Please try again later.",
            "service", service
        ));
    }
}
