package com.homegenie.paymentservice.controller;

import com.homegenie.paymentservice.dto.MiniAppPaymentRequest;
import com.homegenie.paymentservice.dto.MiniAppPaymentResponse;
import com.homegenie.paymentservice.service.PaymentPlatformService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/payments/mini-app")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Payment Platform", description = "Mini-app payment operations with commission tracking")
@SecurityRequirement(name = "bearerAuth")
public class MiniAppPaymentController {

    private final PaymentPlatformService paymentPlatformService;

    
    @PostMapping
    @Operation(
        summary = "Create mini-app payment",
        description = "Create a new payment for any mini-app with automatic commission calculation. " +
                      "The commission rate is configured per mini-app in application.yml. " +
                      "Supports: Maintenance, Marketplace, Booking mini-apps."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Payment created successfully with commission calculated",
            content = @Content(schema = @Schema(implementation = MiniAppPaymentResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or mini-app not found/disabled or duplicate payment"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Stripe API error or internal server error"
        )
    })
    public ResponseEntity<MiniAppPaymentResponse> createPayment(
            @Valid @RequestBody MiniAppPaymentRequest request) {
        
        try {
            log.info("POST /api/payments/mini-app - Mini-App: {}, Order: {}, Amount: {}", 
                    request.getMiniAppId(), request.getOrderId(), request.getAmount());
            
            MiniAppPaymentResponse response = paymentPlatformService.createMiniAppPayment(request);
            
            log.info("Payment created - Payment ID: {}, Commission: {} ({}%)", 
                    response.getId(), response.getCommission(), 
                    response.getCommissionRate().multiply(BigDecimal.valueOf(100)));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (StripeException e) {
            log.error("Stripe API error: {}", e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    
    @GetMapping("/order/{orderId}")
    @Operation(
        summary = "Get payment by order ID",
        description = "Retrieve payment details for a specific order across any mini-app"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment found",
            content = @Content(schema = @Schema(implementation = MiniAppPaymentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<MiniAppPaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        log.info("GET /api/payments/mini-app/order/{}", orderId);
        MiniAppPaymentResponse response = paymentPlatformService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/{miniAppId}")
    @Operation(
        summary = "Get all payments for mini-app",
        description = "Retrieve all payment records for a specific mini-app"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payments retrieved successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MiniAppPaymentResponse.class)))
        ),
        @ApiResponse(responseCode = "400", description = "Mini-app not found or disabled")
    })
    public ResponseEntity<List<MiniAppPaymentResponse>> getPaymentsByMiniApp(
            @PathVariable String miniAppId) {
        
        log.info("GET /api/payments/mini-app/{}", miniAppId);
        List<MiniAppPaymentResponse> payments = paymentPlatformService.getPaymentsByMiniApp(miniAppId);
        return ResponseEntity.ok(payments);
    }

    
    @GetMapping("/{miniAppId}/user/{userId}")
    @Operation(
        summary = "Get user payments in mini-app",
        description = "Retrieve all payments made by a specific user in a mini-app"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User payments retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MiniAppPaymentResponse.class)))
        ),
        @ApiResponse(responseCode = "400", description = "Mini-app not found or disabled")
    })
    public ResponseEntity<List<MiniAppPaymentResponse>> getUserPaymentsInMiniApp(
            @PathVariable String miniAppId,
            @PathVariable Long userId) {
        
        log.info("GET /api/payments/mini-app/{}/user/{}", miniAppId, userId);
        List<MiniAppPaymentResponse> payments = paymentPlatformService
                .getUserPaymentsInMiniApp(userId, miniAppId);
        return ResponseEntity.ok(payments);
    }

    
    @GetMapping("/{miniAppId}/commission")
    @Operation(
        summary = "Get total commission for mini-app",
        description = "Calculate total commission earned from a mini-app (succeeded payments only)"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Commission calculated",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(responseCode = "400", description = "Mini-app not found or disabled")
    })
    public ResponseEntity<Map<String, Object>> getTotalCommission(@PathVariable String miniAppId) {
        log.info("GET /api/payments/mini-app/{}/commission", miniAppId);
        BigDecimal totalCommission = paymentPlatformService.getTotalCommission(miniAppId);
        
        return ResponseEntity.ok(Map.of(
                "miniAppId", miniAppId,
                "totalCommission", totalCommission,
                "currency", "usd"
        ));
    }

    
    @GetMapping("/health")
    @Operation(
        summary = "Payment platform health check",
        description = "Check if payment platform services are operational"
    )
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Payment Platform v2.0",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(
        summary = "Refund mini-app payment",
        description = "Process a full or partial refund for a mini-app payment via Stripe"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Refund processed successfully",
            content = @Content(schema = @Schema(implementation = MiniAppPaymentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "402", description = "Stripe refund error"),
        @ApiResponse(responseCode = "422", description = "Payment not in refundable state")
    })
    public ResponseEntity<MiniAppPaymentResponse> refundPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) java.math.BigDecimal amount) throws Exception {
        log.info("POST /api/payments/mini-app/{}/refund - Amount: {}", paymentId, amount);
        MiniAppPaymentResponse response = paymentPlatformService.refundMiniAppPayment(paymentId, amount);
        return ResponseEntity.ok(response);
    }
}
