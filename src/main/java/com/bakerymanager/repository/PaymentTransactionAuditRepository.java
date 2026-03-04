package com.bakerymanager.repository;

import com.bakerymanager.entity.PaymentTransactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionAuditRepository extends JpaRepository<PaymentTransactionAudit, Long> {

    List<PaymentTransactionAudit> findByPaymentTransactionIdOrderByCreatedAtAsc(Long paymentTransactionId);
}
