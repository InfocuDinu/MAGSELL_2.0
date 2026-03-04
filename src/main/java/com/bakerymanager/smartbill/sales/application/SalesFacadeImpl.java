package com.bakerymanager.smartbill.sales.application;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.PaymentTransaction;
import com.bakerymanager.entity.Sale;
import com.bakerymanager.service.SaleService;
import com.bakerymanager.smartbill.sales.api.SalesFacade;
import com.bakerymanager.smartbill.sales.domain.SalesPort;
import com.bakerymanager.smartbill.sales.domain.event.SaleCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalesFacadeImpl implements SalesFacade {

    private final SalesPort salesPort;
    private final ApplicationEventPublisher eventPublisher;

    public SalesFacadeImpl(SalesPort salesPort,
                           ApplicationEventPublisher eventPublisher) {
        this.salesPort = salesPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<Product> getAvailableProducts() {
        return salesPort.getAvailableProducts();
    }

    @Override
    public Sale createSale(List<SaleService.CartItem> cartItems, String paymentMethod, BigDecimal cashReceived, String operator) {
        Sale sale = salesPort.createSale(cartItems, paymentMethod, cashReceived, operator);
        eventPublisher.publishEvent(new SaleCompletedEvent(
            sale.getId(),
            sale.getTotalAmount(),
            sale.getPaymentMethod(),
            LocalDateTime.now()
        ));
        return sale;
    }

    @Override
    public boolean printFiscalReceipt(Sale sale) {
        return salesPort.printFiscalReceipt(sale);
    }

    @Override
    public String getLastFiscalError() {
        return salesPort.getLastFiscalError();
    }

    @Override
    public List<PaymentTransaction> getPendingPaymentTransactions() {
        return salesPort.getPendingPaymentTransactions();
    }

    @Override
    public PaymentTransaction reconcilePaymentTransaction(Long transactionId, BigDecimal settledAmount, String operator, String details) {
        return salesPort.reconcilePaymentTransaction(transactionId, settledAmount, operator, details);
    }
}
