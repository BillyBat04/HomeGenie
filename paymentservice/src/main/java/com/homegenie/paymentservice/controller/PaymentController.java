package com.homegenie.paymentservice.controller;

import com.homegenie.paymentservice.dto.PaymentRequest;
import com.homegenie.paymentservice.dto.PaymentResponse;
import com.homegenie.paymentservice.model.Payment;
import com.homegenie.paymentservice.service.PaymentService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @Operation(summary = "Create new payment",
        description = "Create Stripe payment intent for maintenance request. " +
                      "Payment is only allowed if request has been assigned to technician " +
                      "and is in IN_PROGRESS or COMPLETED status.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment created successfully",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or Stripe error"),
        @ApiResponse(responseCode = "403", description = "Cannot create payment for another user"),
        @ApiResponse(responseCode = "409", description = "Payment already exists"),
        @ApiResponse(responseCode = "503", description = "Maintenance service unavailable")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PaymentRequest request) throws Exception {

        Long authenticatedUserId = Long.parseLong(jwt.getSubject());

        if (!authenticatedUserId.equals(request.getUserId())) {
            log.warn("Forbidden: JWT sub {} attempted to create payment for user {}",
                    authenticatedUserId, request.getUserId());
            throw new AccessDeniedException("Cannot create payment for another user");
        }

        log.info("Creating payment for user {} and request {}", request.getUserId(), request.getRequestId());
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get payment by ID", description = "Retrieve payment details by payment ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @Operation(summary = "Get user payments", description = "Retrieve all payments for specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payments retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaymentResponse.class))))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }

    @Operation(summary = "Get payment by request", description = "Retrieve payment for maintenance request")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/request/{requestId}")
    public ResponseEntity<PaymentResponse> getPaymentByRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(paymentService.getPaymentByRequestId(requestId));
    }

    @Operation(summary = "Get payments by status", description = "Filter payments by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payments retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaymentResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(@PathVariable String status) {
        Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(paymentStatus));
    }

    @Operation(summary = "Refund payment", description = "Process full or partial refund via Stripe")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refund successful",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "422", description = "Invalid refund state")
    })
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) BigDecimal amount) throws Exception {
        log.info("Refunding payment {} with amount {}", paymentId, amount);
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, amount));
    }

    @Operation(summary = "Cancel payment", description = "Cancel pending payment via Stripe")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment cancelled",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "422", description = "Invalid cancellation state")
    })
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long paymentId) throws Exception {
        log.info("Canceling payment: {}", paymentId);
        return ResponseEntity.ok(paymentService.cancelPayment(paymentId));
    }

    @Operation(summary = "Confirm payment", description = "Confirm payment intent")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment confirmed",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Stripe confirmation error")
    })
    @PostMapping("/confirm/{paymentIntentId}")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable String paymentIntentId) throws Exception {
        log.info("Confirming payment intent: {}", paymentIntentId);
        return ResponseEntity.ok(paymentService.confirmPayment(paymentIntentId));
    }
}

