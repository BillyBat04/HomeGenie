package com.homegenie.paymentservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Invoice creation request")
public class InvoiceRequest {
    
    @Schema(description = "User ID", example = "1001")
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Schema(description = "Maintenance request ID", example = "2001")
    @NotNull(message = "Request ID is required")
    private Long requestId;
    
    @Schema(description = "Subtotal amount", example = "120.00")
    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.01", message = "Subtotal must be at least 0.01")
    private BigDecimal subtotal;
    
    @Schema(description = "Tax amount", example = "12.00")
    private BigDecimal tax = BigDecimal.ZERO;

    @Schema(description = "Late fee amount", example = "10.00")
    private BigDecimal lateFee = BigDecimal.ZERO;
    
    @Schema(description = "Invoice description", example = "Plumbing repair service")
    private String description;
    
    @Schema(description = "Additional notes", example = "Payment due within 7 days")
    private String notes;
    
    @Schema(description = "Due date", example = "2024-01-22T23:59:59")
    @NotNull(message = "Due date is required")
    private LocalDateTime dueAt;
}
