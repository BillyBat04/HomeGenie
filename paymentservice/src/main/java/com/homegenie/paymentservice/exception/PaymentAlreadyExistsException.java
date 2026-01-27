package com.homegenie.paymentservice.exception;

public class PaymentAlreadyExistsException extends RuntimeException {
    public PaymentAlreadyExistsException(Long requestId) {
        super("Payment already exists for request: " + requestId);
    }
}
