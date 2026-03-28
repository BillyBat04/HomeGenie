package com.homegenie.paymentservice.repository;

import com.homegenie.paymentservice.model.Payment;
import com.homegenie.paymentservice.model.Payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByUserId(Long userId);
    
    
    List<Payment> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
    List<Payment> findByMiniAppId(String miniAppId);
    List<Payment> findByUserIdAndMiniAppId(Long userId, String miniAppId);
    
    
    @Deprecated
    default List<Payment> findByRequestId(Long requestId) {
        return findByOrderId(requestId);
    }
    
    @Deprecated
    default boolean existsByRequestId(Long requestId) {
        return existsByOrderId(requestId);
    }
    
    List<Payment> findByStatus(PaymentStatus status);
    
    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);
    
    Optional<Payment> findByStripeChargeId(String chargeId);
    
    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);
}
