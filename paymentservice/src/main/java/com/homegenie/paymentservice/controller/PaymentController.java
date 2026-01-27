package com.homegenie.paymentservice.controller;

import com.homegenie.paymentservice.dto.PaymentRequest;
import com.homegenie.paymentservice.dto.PaymentResponse;
import com.homegenie.paymentservice.model.Payment;
import com.homegenie.paymentservice.service.PaymentService;
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

@RestController
@RequestMapping("/api/payments")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment operations with Stripe integration")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment
     * 
     * BUSINESS RULE: Payment can only be created if maintenance request:
     * 1. Has been assigned to a technician (not in PENDING status)
     * 2. Status is IN_PROGRESS or COMPLETED
     * 3. Payment hasn't been created yet for this request
     */
    @Operation(summary = "Create new payment", 
        description = "Create Stripe payment intent for maintenance request. " +
                      "Payment is only allowed if request has been assigned to technician " +
                      "and is in IN_PROGRESS or COMPLETED status.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment created successfully",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Stripe payment error or invalid request data"),
        @ApiResponse(responseCode = "409", description = "Payment not allowed - request not assigned or already paid"),
        @ApiResponse(responseCode = "503", description = "Maintenance service unavailable")
    })
    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @Valid @RequestBody PaymentRequest request) {
        try {
            // SECURITY: Validate authenticated user matches request user
            if (!authenticatedUserId.equals(request.getUserId())) {
                log.warn("Authorization failed: User {} attempted to create payment for user {}", 
                        authenticatedUserId, request.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Cannot create payment for another user"));
            }
            
            log.info("Creating payment for user {} and request {}", request.getUserId(), request.getRequestId());
            PaymentResponse response = paymentService.createPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (StripeException e) {
            log.error("Stripe error creating payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Stripe payment error: " + e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("Invalid payment state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error creating payment: {}", e.getMessage(), e);
            if (e.getMessage().contains("communicate with Maintenance Service")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Simple error response for payment operations
     */
    @Schema(description = "Error response")
    record ErrorResponse(
        @Schema(description = "Error message", example = "Cannot create payment for PENDING maintenance request")
        String message
    ) {}

    /**
     * Get payment by ID
     */
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details by payment ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        try {
            PaymentResponse response = paymentService.getPaymentById(paymentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Payment not found: {}", paymentId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get payments by user ID
     */
    @Operation(summary = "Get user payments", description = "Retrieve all payments for specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payments retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaymentResponse.class))))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUser(@PathVariable Long userId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment by request ID
     */
    @Operation(summary = "Get payment by request", description = "Retrieve payment for maintenance request")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/request/{requestId}")
    public ResponseEntity<PaymentResponse> getPaymentByRequest(@PathVariable Long requestId) {
        try {
            PaymentResponse response = paymentService.getPaymentByRequestId(requestId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Payment not found for request: {}", requestId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get payments by status
     */
    @Operation(summary = "Get payments by status", description = "Filter payments by status (PENDING/COMPLETED/REFUNDED/CANCELLED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payments retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaymentResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(@PathVariable String status) {
        try {
            Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            List<PaymentResponse> payments = paymentService.getPaymentsByStatus(paymentStatus);
            return ResponseEntity.ok(payments);
        } catch (IllegalArgumentException e) {
            log.error("Invalid payment status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Refund a payment
     */
    @Operation(summary = "Refund payment", description = "Process full or partial refund via Stripe")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refund successful",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Stripe refund error"),
        @ApiResponse(responseCode = "409", description = "Invalid refund state")
    })
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) BigDecimal amount) {
        try {
            log.info("Refunding payment {} with amount {}", paymentId, amount);
            PaymentResponse response = paymentService.refundPayment(paymentId, amount);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error refunding payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            log.error("Invalid refund state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Cancel a payment
     */
    @Operation(summary = "Cancel payment", description = "Cancel pending payment via Stripe")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment cancelled",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Stripe cancellation error"),
        @ApiResponse(responseCode = "409", description = "Invalid cancellation state")
    })
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long paymentId) {
        try {
            log.info("Canceling payment: {}", paymentId);
            PaymentResponse response = paymentService.cancelPayment(paymentId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error canceling payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            log.error("Invalid cancellation state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Confirm a payment (internal use, usually from webhook)
     */
    @Operation(summary = "Confirm payment", description = "Confirm payment intent (webhook callback)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment confirmed",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Stripe confirmation error")
    })
    @PostMapping("/confirm/{paymentIntentId}")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable String paymentIntentId) {
        try {
            log.info("Confirming payment intent: {}", paymentIntentId);
            PaymentResponse response = paymentService.confirmPayment(paymentIntentId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error confirming payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
