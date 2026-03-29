package com.homegenie.paymentservice.service.impl;


import com.homegenie.paymentservice.service.WalletService;
import com.homegenie.paymentservice.dto.WalletDebitRequest;
import com.homegenie.paymentservice.dto.WalletResponse;
import com.homegenie.paymentservice.dto.WalletTopUpRequest;
import com.homegenie.paymentservice.model.Wallet;
import com.homegenie.paymentservice.model.WalletTransaction;
import com.homegenie.paymentservice.repository.WalletRepository;
import com.homegenie.paymentservice.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public WalletResponse createWallet(Long userId, String currency) {
        if (walletRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Wallet already exists for user: " + userId);
        }
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency(currency != null ? currency.toUpperCase() : "USD")
                .active(true)
                .build();
        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet created for user {}: walletId={}", userId, saved.getId());
        return WalletResponse.fromEntity(saved);
    }

    public WalletResponse getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
        return WalletResponse.fromEntity(wallet);
    }

    public WalletResponse getWalletById(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        return WalletResponse.fromEntity(wallet);
    }

    @Transactional
    public WalletResponse topUp(Long walletId, WalletTopUpRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));

        if (!wallet.isActive()) {
            throw new IllegalStateException("Wallet is inactive: " + walletId);
        }

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        recordTransaction(wallet, Wallet.TransactionType.TOP_UP,
                request.getAmount(), wallet.getBalance(),
                request.getReferenceId(), request.getDescription());

        log.info("Wallet {} topped up by {}. New balance: {}", walletId, request.getAmount(), wallet.getBalance());
        return WalletResponse.fromEntity(wallet);
    }

    @Transactional
    public WalletResponse debit(Long walletId, WalletDebitRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));

        if (!wallet.isActive()) {
            throw new IllegalStateException("Wallet is inactive: " + walletId);
        }
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient wallet balance. Available: " + wallet.getBalance()
                    + ", Requested: " + request.getAmount());
        }

        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        recordTransaction(wallet, Wallet.TransactionType.DEBIT,
                request.getAmount(), wallet.getBalance(),
                request.getReferenceId(), request.getDescription());

        log.info("Wallet {} debited by {}. New balance: {}", walletId, request.getAmount(), wallet.getBalance());
        return WalletResponse.fromEntity(wallet);
    }

    public BigDecimal getBalance(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId))
                .getBalance();
    }

    public List<WalletResponse.WalletTransactionDto> getTransactions(Long walletId) {
        if (!walletRepository.existsById(walletId)) {
            throw new IllegalArgumentException("Wallet not found: " + walletId);
        }
        return walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(walletId)
                .stream()
                .map(WalletResponse.WalletTransactionDto::fromEntity)
                .collect(Collectors.toList());
    }

    private void recordTransaction(Wallet wallet, Wallet.TransactionType type,
                                   BigDecimal amount, BigDecimal balanceAfter,
                                   String referenceId, String description) {
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceId(referenceId)
                .description(description)
                .build();
        walletTransactionRepository.save(tx);
    }
}
