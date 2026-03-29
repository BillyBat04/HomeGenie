package com.homegenie.paymentservice.service;

import com.homegenie.paymentservice.dto.InvoiceRequest;
import com.homegenie.paymentservice.dto.InvoiceResponse;
import com.homegenie.paymentservice.model.Invoice;

import java.util.List;

public interface InvoiceService {

    InvoiceResponse createInvoice(InvoiceRequest request);

    InvoiceResponse getInvoiceById(Long invoiceId);

    InvoiceResponse getInvoiceByNumber(String invoiceNumber);

    List<InvoiceResponse> getInvoicesByUserId(Long userId);

    List<InvoiceResponse> getInvoicesByStatus(Invoice.InvoiceStatus status);

    InvoiceResponse sendInvoice(Long invoiceId);

    InvoiceResponse markInvoiceAsViewed(Long invoiceId);

    InvoiceResponse markInvoiceAsPaid(Long invoiceId, Long paymentId);

    InvoiceResponse cancelInvoice(Long invoiceId);

    void processOverdueInvoices();
}
