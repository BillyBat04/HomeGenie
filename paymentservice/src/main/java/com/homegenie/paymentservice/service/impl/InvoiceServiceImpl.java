package com.homegenie.paymentservice.service.impl;


import com.homegenie.paymentservice.service.InvoiceService;
import com.homegenie.paymentservice.dto.InvoiceRequest;
import com.homegenie.paymentservice.dto.InvoiceResponse;
import com.homegenie.paymentservice.model.Invoice;
import com.homegenie.paymentservice.model.Payment;
import com.homegenie.paymentservice.repository.InvoiceRepository;
import com.homegenie.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${invoice.number-prefix:INV}")
    private String invoicePrefix;

    @Value("${invoice.late-fee-percentage:5.0}")
    private BigDecimal lateFeePercentage;

    
    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        log.info("Creating invoice for user {} and request {}", request.getUserId(), request.getRequestId());

        
        String invoiceNumber = generateInvoiceNumber();

        
        BigDecimal subtotal = request.getSubtotal();
        BigDecimal tax = request.getTax() != null ? request.getTax() : BigDecimal.ZERO;
        BigDecimal lateFee = request.getLateFee() != null ? request.getLateFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(tax).add(lateFee);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .userId(request.getUserId())
                .requestId(request.getRequestId())
                .subtotal(subtotal)
                .tax(tax)
                .lateFee(lateFee)
                .totalAmount(totalAmount)
                .status(Invoice.InvoiceStatus.DRAFT)
                .description(request.getDescription())
                .notes(request.getNotes())
                .issuedAt(LocalDateTime.now())
                .dueAt(request.getDueAt())
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice created: {}", invoiceNumber);

        return InvoiceResponse.fromEntity(invoice);
    }

    
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        return InvoiceResponse.fromEntity(invoice);
    }

    
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));
        return InvoiceResponse.fromEntity(invoice);
    }

    
    public List<InvoiceResponse> getInvoicesByUserId(Long userId) {
        List<Invoice> invoices = invoiceRepository.findByUserId(userId);
        return invoices.stream()
                .map(InvoiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    
    public List<InvoiceResponse> getInvoicesByStatus(Invoice.InvoiceStatus status) {
        List<Invoice> invoices = invoiceRepository.findByStatus(status);
        return invoices.stream()
                .map(InvoiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    
    @Transactional
    public InvoiceResponse sendInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Can only send draft invoices");
        }

        invoice.setStatus(Invoice.InvoiceStatus.SENT);
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice sent: {}", invoice.getInvoiceNumber());
        return InvoiceResponse.fromEntity(invoice);
    }

    
    @Transactional
    public InvoiceResponse markInvoiceAsViewed(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == Invoice.InvoiceStatus.SENT) {
            invoice.setStatus(Invoice.InvoiceStatus.VIEWED);
            invoice = invoiceRepository.save(invoice);
            log.info("Invoice viewed: {}", invoice.getInvoiceNumber());
        }

        return InvoiceResponse.fromEntity(invoice);
    }

    
    @Transactional
    public InvoiceResponse markInvoiceAsPaid(Long invoiceId, Long paymentId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Payment must be successful to mark invoice as paid");
        }

        invoice.setPayment(payment);
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice paid: {} with payment: {}", invoice.getInvoiceNumber(), paymentId);
        return InvoiceResponse.fromEntity(invoice);
    }

    
    @Transactional
    public InvoiceResponse cancelInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot cancel paid invoice. Use refund instead.");
        }

        invoice.setStatus(Invoice.InvoiceStatus.CANCELED);
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice canceled: {}", invoice.getInvoiceNumber());
        return InvoiceResponse.fromEntity(invoice);
    }

    
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void processOverdueInvoices() {
        log.info("Processing overdue invoices...");

        LocalDateTime now = LocalDateTime.now();
        List<Invoice> overdueInvoices = invoiceRepository.findByDueAtBeforeAndStatus(
                now, Invoice.InvoiceStatus.SENT);

        for (Invoice invoice : overdueInvoices) {
            
            if (invoice.getLateFee().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal lateFee = invoice.getSubtotal()
                        .multiply(lateFeePercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                invoice.setLateFee(lateFee);
                invoice.setTotalAmount(invoice.getTotalAmount().add(lateFee));
            }

            invoice.setStatus(Invoice.InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);

            log.info("Invoice marked as overdue: {} with late fee: {}",
                    invoice.getInvoiceNumber(), invoice.getLateFee());
        }

        log.info("Processed {} overdue invoices", overdueInvoices.size());
    }

    
    private String generateInvoiceNumber() {
        Long counter = jdbcTemplate.queryForObject(
                "SELECT nextval('invoice_number_seq')", Long.class);
        return String.format("%s-%06d", invoicePrefix, counter);
    }
}
