package com.bakerymanager.repository;

import com.bakerymanager.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionReference(String transactionReference);

    List<PaymentTransaction> findByStatus(PaymentTransaction.TransactionStatus status);

    List<PaymentTransaction> findByReconciliationStatus(PaymentTransaction.ReconciliationStatus reconciliationStatus);
}
