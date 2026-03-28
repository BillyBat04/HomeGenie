package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.InvoiceRequest;
import com.homegenie.paymentservice.dto.InvoiceResponse;
import com.homegenie.paymentservice.model.Invoice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("null")
class InvoiceServiceIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void testCreateInvoice_Success() {
        
        InvoiceRequest request = new InvoiceRequest();
        request.setUserId(1L);
        request.setRequestId(1L);
        request.setSubtotal(new BigDecimal("100.00"));
        request.setTax(new BigDecimal("10.00"));
        request.setDescription("Test invoice");

        
        InvoiceResponse response = invoiceService.createInvoice(request);

        
        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getInvoiceNumber());
        assertTrue(response.getInvoiceNumber().startsWith("INV-"));
        assertEquals(new BigDecimal("110.00"), response.getTotalAmount());
        assertEquals(Invoice.InvoiceStatus.DRAFT, response.getStatus());
    }

    @Test
    void testCreateInvoice_WithLateFee_CalculatesCorrectly() {
        
        InvoiceRequest request = new InvoiceRequest();
        request.setUserId(1L);
        request.setRequestId(2L);
        request.setSubtotal(new BigDecimal("200.00"));
        request.setTax(new BigDecimal("20.00"));
        request.setLateFee(new BigDecimal("10.00"));

        
        InvoiceResponse response = invoiceService.createInvoice(request);

        
        assertEquals(new BigDecimal("230.00"), response.getTotalAmount());
    }

    @Test
    void testSendInvoice_UpdatesStatus() {
        
        InvoiceRequest request = new InvoiceRequest();
        request.setUserId(1L);
        request.setRequestId(3L);
        request.setSubtotal(new BigDecimal("150.00"));
        request.setTax(new BigDecimal("15.00"));

        InvoiceResponse created = invoiceService.createInvoice(request);

        
        InvoiceResponse sent = invoiceService.sendInvoice(created.getId());

        
        assertEquals(Invoice.InvoiceStatus.SENT, sent.getStatus());
        assertNotNull(sent.getIssuedAt());
    }

    @Test
    void testMarkInvoicePaid_UpdatesStatusAndDate() {
        
        InvoiceRequest request = new InvoiceRequest();
        request.setUserId(1L);
        request.setRequestId(4L);
        request.setSubtotal(new BigDecimal("100.00"));

        InvoiceResponse created = invoiceService.createInvoice(request);
        invoiceService.sendInvoice(created.getId());

        
        InvoiceResponse paid = invoiceService.markInvoiceAsPaid(created.getId(), 1L);

        
        assertEquals(Invoice.InvoiceStatus.PAID, paid.getStatus());
        assertNotNull(paid.getPaidAt());
        assertEquals(1L, paid.getPaymentId());
    }

    @Test
    void testGetInvoicesByUser_ReturnsUserInvoices() {
        
        Long userId = 5L;

        InvoiceRequest request1 = new InvoiceRequest();
        request1.setUserId(userId);
        request1.setRequestId(10L);
        request1.setSubtotal(new BigDecimal("100.00"));

        InvoiceRequest request2 = new InvoiceRequest();
        request2.setUserId(userId);
        request2.setRequestId(11L);
        request2.setSubtotal(new BigDecimal("200.00"));

        invoiceService.createInvoice(request1);
        invoiceService.createInvoice(request2);

        
        var invoices = invoiceService.getInvoicesByUserId(userId);

        
        assertTrue(invoices.size() >= 2);
        assertTrue(invoices.stream().allMatch(inv -> inv.getUserId().equals(userId)));
    }

    @Test
    void testProcessOverdueInvoices_AddsLateFees() {
        
        InvoiceRequest request = new InvoiceRequest();
        request.setUserId(1L);
        request.setRequestId(12L);
        request.setSubtotal(new BigDecimal("100.00"));
        request.setDueAt(LocalDateTime.now().minusDays(1)); 

        InvoiceResponse created = invoiceService.createInvoice(request);
        invoiceService.sendInvoice(created.getId());

        
        invoiceService.processOverdueInvoices();

        
        InvoiceResponse updated = invoiceService.getInvoiceById(created.getId());
        assertEquals(Invoice.InvoiceStatus.OVERDUE, updated.getStatus());
        assertTrue(updated.getLateFee().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testUniqueInvoiceNumbers_NoDuplicates() {
        
        InvoiceRequest request1 = new InvoiceRequest();
        request1.setUserId(1L);
        request1.setRequestId(20L);
        request1.setSubtotal(new BigDecimal("100.00"));

        InvoiceRequest request2 = new InvoiceRequest();
        request2.setUserId(2L);
        request2.setRequestId(21L);
        request2.setSubtotal(new BigDecimal("200.00"));

        InvoiceResponse inv1 = invoiceService.createInvoice(request1);
        InvoiceResponse inv2 = invoiceService.createInvoice(request2);

        
        assertNotEquals(inv1.getInvoiceNumber(), inv2.getInvoiceNumber());
    }
}

