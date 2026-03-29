package com.homegenie.paymentservice.controller;

import com.homegenie.paymentservice.dto.InvoiceRequest;
import com.homegenie.paymentservice.dto.InvoiceResponse;
import com.homegenie.paymentservice.model.Invoice;
import com.homegenie.paymentservice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Invoice", description = "Invoice management operations")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    
    @Operation(summary = "Create new invoice", description = "Generate invoice for maintenance request")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invoice created successfully",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        try {
            log.info("Creating invoice for user {} and request {}", request.getUserId(), request.getRequestId());
            InvoiceResponse response = invoiceService.createInvoice(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    @Operation(summary = "Get invoice by ID", description = "Retrieve invoice details by invoice ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice found",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long invoiceId) {
        try {
            InvoiceResponse response = invoiceService.getInvoiceById(invoiceId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Invoice not found: {}", invoiceId);
            return ResponseEntity.notFound().build();
        }
    }

    
    @Operation(summary = "Get invoice by number", description = "Retrieve invoice by invoice number (e.g., INV-001)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice found",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        try {
            InvoiceResponse response = invoiceService.getInvoiceByNumber(invoiceNumber);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Invoice not found: {}", invoiceNumber);
            return ResponseEntity.notFound().build();
        }
    }

    
    @Operation(summary = "Get user invoices", description = "Retrieve all invoices for specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoices retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = InvoiceResponse.class))))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByUser(@PathVariable Long userId) {
        List<InvoiceResponse> invoices = invoiceService.getInvoicesByUserId(userId);
        return ResponseEntity.ok(invoices);
    }

    
    @Operation(summary = "Get invoices by status", description = "Filter invoices by status (DRAFT/SENT/VIEWED/PAID/CANCELLED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoices retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = InvoiceResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStatus(@PathVariable String status) {
        try {
            Invoice.InvoiceStatus invoiceStatus = Invoice.InvoiceStatus.valueOf(status.toUpperCase());
            List<InvoiceResponse> invoices = invoiceService.getInvoicesByStatus(invoiceStatus);
            return ResponseEntity.ok(invoices);
        } catch (IllegalArgumentException e) {
            log.error("Invalid invoice status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    
    @Operation(summary = "Send invoice", description = "Send invoice to customer via notification")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice sent",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "404", description = "Invoice not found"),
        @ApiResponse(responseCode = "409", description = "Invalid invoice state")
    })
    @PostMapping("/{invoiceId}/send")
    public ResponseEntity<InvoiceResponse> sendInvoice(@PathVariable Long invoiceId) {
        try {
            log.info("Sending invoice: {}", invoiceId);
            InvoiceResponse response = invoiceService.sendInvoice(invoiceId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("Invalid invoice state for sending: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("Invoice not found: {}", invoiceId);
            return ResponseEntity.notFound().build();
        }
    }

    
    @Operation(summary = "Mark invoice viewed", description = "Update invoice status to viewed")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @PostMapping("/{invoiceId}/viewed")
    public ResponseEntity<InvoiceResponse> markInvoiceAsViewed(@PathVariable Long invoiceId) {
        try {
            InvoiceResponse response = invoiceService.markInvoiceAsViewed(invoiceId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Invoice not found: {}", invoiceId);
            return ResponseEntity.notFound().build();
        }
    }

    
    @Operation(summary = "Mark invoice paid", description = "Update invoice status to paid with payment ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice marked paid",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "404", description = "Invoice or payment not found"),
        @ApiResponse(responseCode = "409", description = "Invalid state for payment")
    })
    @PostMapping("/{invoiceId}/paid")
    public ResponseEntity<InvoiceResponse> markInvoiceAsPaid(
            @PathVariable Long invoiceId,
            @RequestParam Long paymentId) {
        try {
            log.info("Marking invoice {} as paid with payment {}", invoiceId, paymentId);
            InvoiceResponse response = invoiceService.markInvoiceAsPaid(invoiceId, paymentId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("Invalid state for marking invoice as paid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("Invoice or payment not found");
            return ResponseEntity.notFound().build();
        }
    }

    
    @Operation(summary = "Cancel invoice", description = "Cancel unpaid invoice")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice cancelled",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "404", description = "Invoice not found"),
        @ApiResponse(responseCode = "409", description = "Invalid state for cancellation")
    })
    @PostMapping("/{invoiceId}/cancel")
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable Long invoiceId) {
        try {
            log.info("Canceling invoice: {}", invoiceId);
            InvoiceResponse response = invoiceService.cancelInvoice(invoiceId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("Invalid state for canceling invoice: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("Invoice not found: {}", invoiceId);
            return ResponseEntity.notFound().build();
        }
    }
}
