package com.homegenie.paymentservice.repository;

import com.homegenie.paymentservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByTransactionId(String transactionId);
    
    List<Transaction> findByPaymentId(Long paymentId);
    
    List<Transaction> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}
