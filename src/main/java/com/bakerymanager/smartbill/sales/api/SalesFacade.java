package com.bakerymanager.smartbill.sales.api;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.PaymentTransaction;
import com.bakerymanager.entity.Sale;
import com.bakerymanager.service.SaleService;

import java.math.BigDecimal;
import java.util.List;

public interface SalesFacade {

    List<Product> getAvailableProducts();

    Sale createSale(List<SaleService.CartItem> cartItems, String paymentMethod, BigDecimal cashReceived, String operator);

    boolean printFiscalReceipt(Sale sale);

    String getLastFiscalError();

    List<PaymentTransaction> getPendingPaymentTransactions();

    PaymentTransaction reconcilePaymentTransaction(Long transactionId, BigDecimal settledAmount, String operator, String details);
}
