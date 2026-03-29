package com.homegenie.paymentservice.dto;

import com.homegenie.paymentservice.model.Wallet;
import com.homegenie.paymentservice.model.WalletTransaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class WalletResponse {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private String currency;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WalletTransactionDto> recentTransactions;

    public static WalletResponse fromEntity(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .active(wallet.isActive())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Data
    @Builder
    public static class WalletTransactionDto {
        private Long id;
        private String transactionType;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String referenceId;
        private String description;
        private LocalDateTime createdAt;

        public static WalletTransactionDto fromEntity(WalletTransaction tx) {
            return WalletTransactionDto.builder()
                    .id(tx.getId())
                    .transactionType(tx.getTransactionType().name())
                    .amount(tx.getAmount())
                    .balanceAfter(tx.getBalanceAfter())
                    .referenceId(tx.getReferenceId())
                    .description(tx.getDescription())
                    .createdAt(tx.getCreatedAt())
                    .build();
        }
    }
}
