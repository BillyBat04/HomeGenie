package com.homegenie.paymentservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
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

    
    private Counter paymentCreatedCounter;
    private Counter paymentSuccessCounter;
    private Counter paymentFailedCounter;
    private Timer paymentProcessingTimer;
    private DistributionSummary paymentAmountSummary;

    
    private Counter walletCreatedCounter;
    private Counter walletTopUpCounter;
    private Counter walletDebitCounter;
    private Counter walletTransferCounter;
    private DistributionSummary walletBalanceSummary;

    
    private Counter invoiceCreatedCounter;
    private Counter invoicePaidCounter;
    private Counter invoiceOverdueCounter;
    private DistributionSummary invoiceAmountSummary;

    
    private Counter stripeApiCallCounter;
    private Counter stripeApiErrorCounter;
    private Timer stripeApiTimer;

    @PostConstruct
    public void init() {
        log.info("Initializing Payment Service custom metrics...");

        
        paymentCreatedCounter = Counter.builder("payment_created_total")
                .description("Total number of payments created")
                .tag("service", "payment-service")
                .tag("operation", "payment")
                .register(registry);

        paymentSuccessCounter = Counter.builder("payment_completed_total")
                .description("Total number of successful payments")
                .tag("service", "payment-service")
                .tag("operation", "payment")
                .tag("status", "success")
                .register(registry);

        paymentFailedCounter = Counter.builder("payment_completed_total")
                .description("Total number of failed payments")
                .tag("service", "payment-service")
                .tag("operation", "payment")
                .tag("status", "failed")
                .register(registry);

        paymentProcessingTimer = Timer.builder("payment_processing_duration_seconds")
                .description("Time taken to process a payment")
                .tag("service", "payment-service")
                .tag("operation", "payment")
                .register(registry);

        paymentAmountSummary = DistributionSummary.builder("payment_amount_cents")
                .description("Distribution of payment amounts")
                .tag("service", "payment-service")
                .tag("operation", "payment")
                .baseUnit("cents")
                .register(registry);

        
        walletCreatedCounter = Counter.builder("wallet_created_total")
                .description("Total number of wallets created")
                .tag("service", "payment-service")
                .tag("operation", "wallet")
                .register(registry);

        walletTopUpCounter = Counter.builder("wallet_topup_total")
                .description("Total number of wallet top-ups")
                .tag("service", "payment-service")
                .tag("operation", "topup")
                .register(registry);

        walletDebitCounter = Counter.builder("wallet_debit_total")
                .description("Total number of wallet debits")
                .tag("service", "payment-service")
                .tag("operation", "debit")
                .register(registry);

        walletTransferCounter = Counter.builder("wallet_transfer_total")
                .description("Total number of wallet transfers")
                .tag("service", "payment-service")
                .tag("operation", "transfer")
                .register(registry);

        walletBalanceSummary = DistributionSummary.builder("wallet_balance_cents")
                .description("Distribution of wallet balances")
                .tag("service", "payment-service")
                .tag("operation", "wallet")
                .baseUnit("cents")
                .register(registry);

        
        invoiceCreatedCounter = Counter.builder("invoice_created_total")
                .description("Total number of invoices created")
                .tag("service", "payment-service")
                .tag("operation", "invoice")
                .register(registry);

        invoicePaidCounter = Counter.builder("invoice_paid_total")
                .description("Total number of invoices paid")
                .tag("service", "payment-service")
                .tag("operation", "invoice")
                .tag("status", "paid")
                .register(registry);

        invoiceOverdueCounter = Counter.builder("invoice_overdue_total")
                .description("Total number of overdue invoices")
                .tag("service", "payment-service")
                .tag("operation", "invoice")
                .tag("status", "overdue")
                .register(registry);

        invoiceAmountSummary = DistributionSummary.builder("invoice_amount_cents")
                .description("Distribution of invoice amounts")
                .tag("service", "payment-service")
                .tag("operation", "invoice")
                .baseUnit("cents")
                .register(registry);

        
        stripeApiCallCounter = Counter.builder("stripe_api_calls_total")
                .description("Total number of Stripe API calls")
                .tag("service", "payment-service")
                .tag("integration", "stripe")
                .register(registry);

        stripeApiErrorCounter = Counter.builder("stripe_api_errors_total")
                .description("Total number of Stripe API errors")
                .tag("service", "payment-service")
                .tag("integration", "stripe")
                .register(registry);

        stripeApiTimer = Timer.builder("stripe_api_duration_seconds")
                .description("Time taken for Stripe API calls")
                .tag("service", "payment-service")
                .tag("integration", "stripe")
                .register(registry);

        log.info("✅ Payment Service custom metrics initialized successfully");
    }

    
    public void recordPaymentCreated() {
        paymentCreatedCounter.increment();
    }

    public void recordPaymentSuccess(long amountInCents) {
        paymentSuccessCounter.increment();
        paymentAmountSummary.record(amountInCents);
    }

    public void recordPaymentFailed() {
        paymentFailedCounter.increment();
    }

    public void recordPaymentProcessingTime(Runnable operation) {
        paymentProcessingTimer.record(operation);
    }

    
    public void recordWalletCreated() {
        walletCreatedCounter.increment();
    }

    public void recordWalletTopUp(long amountInCents) {
        walletTopUpCounter.increment();
        walletBalanceSummary.record(amountInCents);
    }

    public void recordWalletDebit(long amountInCents) {
        walletDebitCounter.increment();
    }

    public void recordWalletTransfer() {
        walletTransferCounter.increment();
    }

    
    public void recordInvoiceCreated(long amountInCents) {
        invoiceCreatedCounter.increment();
        invoiceAmountSummary.record(amountInCents);
    }

    public void recordInvoicePaid() {
        invoicePaidCounter.increment();
    }

    public void recordInvoiceOverdue() {
        invoiceOverdueCounter.increment();
    }

    
    public void recordStripeApiCall() {
        stripeApiCallCounter.increment();
    }

    public void recordStripeApiError() {
        stripeApiErrorCounter.increment();
    }

    public void recordStripeApiTime(Runnable operation) {
        stripeApiTimer.record(operation);
    }
}
