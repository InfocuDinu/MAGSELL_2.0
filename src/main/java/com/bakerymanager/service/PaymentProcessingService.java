package com.bakerymanager.service;

import com.bakerymanager.entity.PaymentTransaction;
import com.bakerymanager.entity.PaymentTransactionAudit;
import com.bakerymanager.repository.PaymentTransactionAuditRepository;
import com.bakerymanager.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PaymentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessingService.class);

    private static final String CASH = "Numerar";
    private static final String CARD = "Card Bancar";
    private static final String TICKETS = "Tichete Mese";
    private static final String OTHER = "Altele";

    private static final Set<String> SUPPORTED_METHODS = Set.of(CASH, CARD, TICKETS, OTHER);

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentTransactionAuditRepository auditRepository;

    public PaymentProcessingService(PaymentTransactionRepository transactionRepository,
                                    PaymentTransactionAuditRepository auditRepository) {
        this.transactionRepository = transactionRepository;
        this.auditRepository = auditRepository;
    }

    public PaymentTransaction processPayment(String paymentMethod,
                                             BigDecimal amount,
                                             BigDecimal receivedAmount,
                                             String operator) {
        validateInput(paymentMethod, amount, receivedAmount);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setPaymentMethod(paymentMethod);
        transaction.setAmount(scale(amount));
        transaction.setReceivedAmount(scale(receivedAmount));
        transaction.setChangeAmount(scale(receivedAmount.subtract(amount).max(BigDecimal.ZERO)));
        transaction.setProviderMessage("Payment initiated");

        transaction = transactionRepository.save(transaction);
        writeAudit(transaction, "INITIATE", null, PaymentTransaction.TransactionStatus.INITIATED.name(),
            "Inițiere tranzacție", operator);

        if (CASH.equals(paymentMethod)) {
            return captureCash(transaction, operator);
        }

        return authorizeAndCaptureNonCash(transaction, operator);
    }

    public void linkSale(Long transactionId, Long saleId, String operator) {
        if (transactionId == null || saleId == null) {
            return;
        }

        PaymentTransaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Tranzacția de plată nu există: " + transactionId));

        transaction.setSaleId(saleId);
        transactionRepository.save(transaction);

        writeAudit(transaction, "LINK_SALE", transaction.getStatus().name(), transaction.getStatus().name(),
            "Asociere tranzacție cu vânzarea " + saleId, operator);
    }

    public PaymentTransaction reconcileTransaction(Long transactionId,
                                                   BigDecimal settledAmount,
                                                   String operator,
                                                   String details) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Tranzacția de plată nu există: " + transactionId));

        if (settledAmount == null || settledAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Suma reconciliată este invalidă.");
        }

        BigDecimal normalizedSettled = scale(settledAmount);
        String previous = transaction.getStatus().name();

        if (normalizedSettled.compareTo(scale(transaction.getAmount())) == 0) {
            transaction.setReconciliationStatus(PaymentTransaction.ReconciliationStatus.MATCHED);
            transaction.setStatus(PaymentTransaction.TransactionStatus.RECONCILED);
            transaction.setProviderMessage("Reconciliere OK");
        } else {
            transaction.setReconciliationStatus(PaymentTransaction.ReconciliationStatus.MISMATCHED);
            transaction.setProviderMessage("Diferență la reconciliere: expected="
                + scale(transaction.getAmount()) + ", settled=" + normalizedSettled);
        }

        transaction.setReconciledAt(LocalDateTime.now());
        transaction.setReconciledBy(operator);
        transactionRepository.save(transaction);

        writeAudit(transaction, "RECONCILE", previous, transaction.getStatus().name(),
            details != null ? details : transaction.getProviderMessage(), operator);

        return transaction;
    }

    public List<PaymentTransaction> getPendingTransactions() {
        return transactionRepository.findByReconciliationStatus(PaymentTransaction.ReconciliationStatus.PENDING)
            .stream()
            .filter(tx -> tx.getStatus() == PaymentTransaction.TransactionStatus.CAPTURED
                || tx.getStatus() == PaymentTransaction.TransactionStatus.AUTHORIZED)
            .sorted(Comparator.comparing(PaymentTransaction::getCreatedAt).reversed())
            .toList();
    }

    private PaymentTransaction captureCash(PaymentTransaction transaction, String operator) {
        updateStatus(transaction, PaymentTransaction.TransactionStatus.AUTHORIZED,
            "Cash amount validated", operator, "AUTHORIZE");
        updateStatus(transaction, PaymentTransaction.TransactionStatus.CAPTURED,
            "Cash payment captured", operator, "CAPTURE");

        transaction.setReconciliationStatus(PaymentTransaction.ReconciliationStatus.MATCHED);
        transaction.setStatus(PaymentTransaction.TransactionStatus.RECONCILED);
        transaction.setReconciledAt(LocalDateTime.now());
        transaction.setReconciledBy(operator);
        transaction.setProviderMessage("Cash payment auto-reconciled");
        transaction = transactionRepository.save(transaction);

        writeAudit(transaction, "RECONCILE", PaymentTransaction.TransactionStatus.CAPTURED.name(),
            PaymentTransaction.TransactionStatus.RECONCILED.name(), "Auto-reconciliere pentru numerar", operator);

        return transaction;
    }

    private PaymentTransaction authorizeAndCaptureNonCash(PaymentTransaction transaction, String operator) {
        updateStatus(transaction, PaymentTransaction.TransactionStatus.AUTHORIZED,
            "Authorization simulated", operator, "AUTHORIZE");
        updateStatus(transaction, PaymentTransaction.TransactionStatus.CAPTURED,
            "Capture simulated", operator, "CAPTURE");

        transaction.setProviderMessage("Tranzacție capturată; în așteptare reconciliere");
        return transactionRepository.save(transaction);
    }

    private void validateInput(String paymentMethod, BigDecimal amount, BigDecimal receivedAmount) {
        if (paymentMethod == null || paymentMethod.isBlank() || !SUPPORTED_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Metoda de plată nu este suportată.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Totalul tranzacției trebuie să fie mai mare decât 0.");
        }

        if (receivedAmount == null || receivedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Suma primită este invalidă.");
        }

        if (CASH.equals(paymentMethod) && receivedAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Pentru numerar, suma primită trebuie să acopere totalul.");
        }

        if (!CASH.equals(paymentMethod) && receivedAmount.compareTo(amount) != 0) {
            throw new IllegalArgumentException("Pentru plățile non-numerar, suma trebuie să fie exact totalul.");
        }
    }

    private void updateStatus(PaymentTransaction transaction,
                              PaymentTransaction.TransactionStatus newStatus,
                              String message,
                              String operator,
                              String action) {
        String previous = transaction.getStatus().name();
        transaction.setStatus(newStatus);
        transaction.setProviderMessage(message);
        transactionRepository.save(transaction);
        writeAudit(transaction, action, previous, newStatus.name(), message, operator);
    }

    private void writeAudit(PaymentTransaction transaction,
                            String action,
                            String oldStatus,
                            String newStatus,
                            String details,
                            String operator) {
        PaymentTransactionAudit audit = new PaymentTransactionAudit();
        audit.setPaymentTransaction(transaction);
        audit.setAction(action);
        audit.setOldStatus(oldStatus);
        audit.setNewStatus(newStatus);
        audit.setDetails(details);
        audit.setActor(operator);
        auditRepository.save(audit);

        logger.info("[PAYMENT_AUDIT] txRef={} action={} from={} to={} actor={} details={}",
            transaction.getTransactionReference(), action, oldStatus, newStatus, operator, details);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
