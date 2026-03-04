package com.bakerymanager.service;

import com.bakerymanager.entity.PaymentTransaction;
import com.bakerymanager.repository.PaymentTransactionAuditRepository;
import com.bakerymanager.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private PaymentTransactionAuditRepository auditRepository;

    @Test
    void shouldAutoReconcileCashPayment() {
        PaymentProcessingService service = new PaymentProcessingService(transactionRepository, auditRepository);
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(1L);
            }
            return tx;
        });

        PaymentTransaction tx = service.processPayment(
            "Numerar",
            new BigDecimal("50.00"),
            new BigDecimal("60.00"),
            "tester"
        );

        assertEquals(PaymentTransaction.TransactionStatus.RECONCILED, tx.getStatus());
        assertEquals(PaymentTransaction.ReconciliationStatus.MATCHED, tx.getReconciliationStatus());
        assertEquals(new BigDecimal("10.00"), tx.getChangeAmount());
    }

    @Test
    void shouldCaptureCardPaymentAndKeepPendingReconciliation() {
        PaymentProcessingService service = new PaymentProcessingService(transactionRepository, auditRepository);
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(1L);
            }
            return tx;
        });

        PaymentTransaction tx = service.processPayment(
            "Card Bancar",
            new BigDecimal("50.00"),
            new BigDecimal("50.00"),
            "tester"
        );

        assertEquals(PaymentTransaction.TransactionStatus.CAPTURED, tx.getStatus());
        assertEquals(PaymentTransaction.ReconciliationStatus.PENDING, tx.getReconciliationStatus());
        assertEquals(new BigDecimal("0.00"), tx.getChangeAmount());
    }

    @Test
    void shouldRejectInsufficientCash() {
        PaymentProcessingService service = new PaymentProcessingService(transactionRepository, auditRepository);
        assertThrows(IllegalArgumentException.class, () -> service.processPayment(
            "Numerar",
            new BigDecimal("100.00"),
            new BigDecimal("70.00"),
            "tester"
        ));
    }

    @Test
    void shouldRejectNonCashWithDifferentReceivedAmount() {
        PaymentProcessingService service = new PaymentProcessingService(transactionRepository, auditRepository);
        assertThrows(IllegalArgumentException.class, () -> service.processPayment(
            "Card Bancar",
            new BigDecimal("100.00"),
            new BigDecimal("99.00"),
            "tester"
        ));
    }

    @Test
    void shouldReturnOnlyPendingCapturedOrAuthorizedTransactionsOrderedByNewest() {
        PaymentProcessingService service = new PaymentProcessingService(transactionRepository, auditRepository);

        PaymentTransaction authorized = new PaymentTransaction();
        authorized.setId(1L);
        authorized.setTransactionReference("PAY-11111111");
        authorized.setStatus(PaymentTransaction.TransactionStatus.AUTHORIZED);
        authorized.setReconciliationStatus(PaymentTransaction.ReconciliationStatus.PENDING);
        authorized.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        PaymentTransaction captured = new PaymentTransaction();
        captured.setId(2L);
        captured.setTransactionReference("PAY-22222222");
        captured.setStatus(PaymentTransaction.TransactionStatus.CAPTURED);
        captured.setReconciliationStatus(PaymentTransaction.ReconciliationStatus.PENDING);
        captured.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        PaymentTransaction initiated = new PaymentTransaction();
        initiated.setId(3L);
        initiated.setTransactionReference("PAY-33333333");
        initiated.setStatus(PaymentTransaction.TransactionStatus.INITIATED);
        initiated.setReconciliationStatus(PaymentTransaction.ReconciliationStatus.PENDING);
        initiated.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        when(transactionRepository.findByReconciliationStatus(PaymentTransaction.ReconciliationStatus.PENDING))
            .thenReturn(List.of(authorized, captured, initiated));

        List<PaymentTransaction> result = service.getPendingTransactions();

        assertEquals(2, result.size());
        assertEquals(captured.getId(), result.get(0).getId());
        assertEquals(authorized.getId(), result.get(1).getId());
    }
}
