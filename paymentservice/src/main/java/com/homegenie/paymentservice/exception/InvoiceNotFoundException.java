package com.homegenie.paymentservice.exception;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(String message) {
        super(message);
    }
    
    public InvoiceNotFoundException(Long invoiceId) {
        super("Invoice not found: " + invoiceId);
    }
}
