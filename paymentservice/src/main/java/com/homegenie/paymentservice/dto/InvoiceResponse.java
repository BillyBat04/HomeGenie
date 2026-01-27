package com.homegenie.paymentservice.dto;

import com.homegenie.paymentservice.model.Invoice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Invoice response data")
public class InvoiceResponse {
    
    @Schema(description = "Invoice ID", example = "1")
    private Long id;
    
    @Schema(description = "Invoice number", example = "INV-2024-001")
    private String invoiceNumber;
    
    @Schema(description = "User ID", example = "1001")
    private Long userId;
    
    @Schema(description = "Maintenance request ID", example = "2001")
    private Long requestId;
    
    @Schema(description = "Associated payment ID", example = "3001")
    private Long paymentId;
    
    @Schema(description = "Subtotal amount", example = "120.00")
    private BigDecimal subtotal;
    
    @Schema(description = "Tax amount", example = "12.00")
    private BigDecimal tax;
    
    @Schema(description = "Late fee", example = "6.00")
    private BigDecimal lateFee;
    
    @Schema(description = "Total amount", example = "138.00")
    private BigDecimal totalAmount;
    
    @Schema(description = "Invoice status", example = "PAID", allowableValues = {"DRAFT", "SENT", "VIEWED", "PAID", "CANCELLED"})
    private String status;
    
    @Schema(description = "Invoice description", example = "Plumbing repair service")
    private String description;
    
    @Schema(description = "Additional notes", example = "Payment due within 7 days")
    private String notes;
    
    @Schema(description = "Issue date", example = "2024-01-15T10:00:00")
    private LocalDateTime issuedAt;
    
    @Schema(description = "Due date", example = "2024-01-22T23:59:59")
    private LocalDateTime dueAt;
    
    @Schema(description = "Payment date", example = "2024-01-20T14:30:00")
    private LocalDateTime paidAt;
    
    @Schema(description = "Creation date", example = "2024-01-15T10:00:00")
    private LocalDateTime createdAt;
    
    public static InvoiceResponse fromEntity(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .userId(invoice.getUserId())
                .requestId(invoice.getRequestId())
                .paymentId(invoice.getPayment() != null ? invoice.getPayment().getId() : null)
                .subtotal(invoice.getSubtotal())
                .tax(invoice.getTax())
                .lateFee(invoice.getLateFee())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus().name())
                .description(invoice.getDescription())
                .notes(invoice.getNotes())
                .issuedAt(invoice.getIssuedAt())
                .dueAt(invoice.getDueAt())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
