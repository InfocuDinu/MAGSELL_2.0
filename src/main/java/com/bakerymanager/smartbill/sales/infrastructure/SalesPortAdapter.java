package com.bakerymanager.smartbill.sales.infrastructure;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.Sale;
import com.bakerymanager.service.FiscalPrinterService;
import com.bakerymanager.service.ProductService;
import com.bakerymanager.service.SaleService;
import com.bakerymanager.smartbill.sales.domain.SalesPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SalesPortAdapter implements SalesPort {

    private final ProductService productService;
    private final SaleService saleService;
    private final FiscalPrinterService fiscalPrinterService;

    public SalesPortAdapter(ProductService productService,
                            SaleService saleService,
                            FiscalPrinterService fiscalPrinterService) {
        this.productService = productService;
        this.saleService = saleService;
        this.fiscalPrinterService = fiscalPrinterService;
    }

    @Override
    public List<Product> getAvailableProducts() {
        return productService.getAvailableProducts();
    }

    @Override
    public Sale createSale(List<SaleService.CartItem> cartItems, String paymentMethod, BigDecimal cashReceived, String operator) {
        return saleService.createSale(cartItems, paymentMethod, cashReceived, operator);
    }

    @Override
    public boolean printFiscalReceipt(Sale sale) {
        return fiscalPrinterService.printReceipt(sale);
    }

    @Override
    public String getLastFiscalError() {
        return fiscalPrinterService.getLastError();
    }
}
