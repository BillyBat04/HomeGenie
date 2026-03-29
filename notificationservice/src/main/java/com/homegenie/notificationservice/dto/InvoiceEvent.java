package com.homegenie.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEvent {
    private Long invoiceId;
    private String invoiceNumber;
    private Long userId;
    private Long requestId;
    private Long paymentId;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal lateFee;
    private BigDecimal totalAmount;
    private String status; 
    private LocalDateTime dueDate;
    private LocalDateTime paidDate;
    private LocalDateTime eventTime;
    private String eventType; 
}
