package com.homegenie.paymentservice.repository;

import com.homegenie.paymentservice.model.Invoice;
import com.homegenie.paymentservice.model.Invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    List<Invoice> findByUserId(Long userId);
    
    List<Invoice> findByStatus(InvoiceStatus status);
    
    List<Invoice> findByUserIdAndStatus(Long userId, InvoiceStatus status);
    
    List<Invoice> findByDueAtBeforeAndStatus(LocalDateTime date, InvoiceStatus status);
    
    Optional<Invoice> findByPaymentId(Long paymentId);
}
