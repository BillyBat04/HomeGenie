package com.homegenie.paymentservice.controller;

import com.homegenie.paymentservice.dto.WalletDebitRequest;
import com.homegenie.paymentservice.dto.WalletResponse;
import com.homegenie.paymentservice.dto.WalletTopUpRequest;
import com.homegenie.paymentservice.service.WalletService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Wallet Operations", description = "In-app wallet management")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "Create wallet", description = "Create a new wallet for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Wallet created",
            content = @Content(schema = @Schema(implementation = WalletResponse.class))),
        @ApiResponse(responseCode = "409", description = "Wallet already exists for this user")
    })
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "USD") String currency) {
        Long userId = Long.parseLong(jwt.getSubject());
        log.info("Creating wallet for user {}", userId);
        WalletResponse response = walletService.createWallet(userId, currency);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get wallet by user ID", description = "Retrieve wallet for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wallet found",
            content = @Content(schema = @Schema(implementation = WalletResponse.class))),
        @ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @Operation(summary = "Get wallet by ID")
    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getWalletById(walletId));
    }

    @Operation(summary = "Top up wallet", description = "Add funds to wallet")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Top-up successful",
            content = @Content(schema = @Schema(implementation = WalletResponse.class))),
        @ApiResponse(responseCode = "404", description = "Wallet not found"),
        @ApiResponse(responseCode = "422", description = "Wallet is inactive")
    })
    @PostMapping("/{walletId}/topup")
    public ResponseEntity<WalletResponse> topUp(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletTopUpRequest request) {
        log.info("Top-up wallet {} amount {}", walletId, request.getAmount());
        return ResponseEntity.ok(walletService.topUp(walletId, request));
    }

    @Operation(summary = "Debit wallet", description = "Deduct funds from wallet")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Debit successful",
            content = @Content(schema = @Schema(implementation = WalletResponse.class))),
        @ApiResponse(responseCode = "404", description = "Wallet not found"),
        @ApiResponse(responseCode = "422", description = "Insufficient balance or inactive wallet")
    })
    @PostMapping("/{walletId}/debit")
    public ResponseEntity<WalletResponse> debit(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletDebitRequest request) {
        log.info("Debit wallet {} amount {}", walletId, request.getAmount());
        return ResponseEntity.ok(walletService.debit(walletId, request));
    }

    @Operation(summary = "Get wallet balance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Balance retrieved"),
        @ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable Long walletId) {
        BigDecimal balance = walletService.getBalance(walletId);
        return ResponseEntity.ok(Map.of("walletId", walletId, "balance", balance));
    }

    @Operation(summary = "Get wallet transactions", description = "List all transactions for a wallet")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transactions retrieved",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = WalletResponse.WalletTransactionDto.class)))),
        @ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<List<WalletResponse.WalletTransactionDto>> getTransactions(
            @PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getTransactions(walletId));
    }
}
