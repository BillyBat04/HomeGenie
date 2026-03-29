package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.WalletDebitRequest;
import com.homegenie.paymentservice.dto.WalletResponse;
import com.homegenie.paymentservice.dto.WalletTopUpRequest;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    WalletResponse createWallet(Long userId, String currency);

    WalletResponse getWalletByUserId(Long userId);

    WalletResponse getWalletById(Long walletId);

    WalletResponse topUp(Long walletId, WalletTopUpRequest request);

    WalletResponse debit(Long walletId, WalletDebitRequest request);

    BigDecimal getBalance(Long walletId);

    List<WalletResponse.WalletTransactionDto> getTransactions(Long walletId);
}
