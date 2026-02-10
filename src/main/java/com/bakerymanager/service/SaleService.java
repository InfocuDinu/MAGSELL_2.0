package com.bakerymanager.service;

import com.bakerymanager.entity.Sale;
import com.bakerymanager.entity.SaleItem;
import com.bakerymanager.entity.Product;
import com.bakerymanager.repository.SaleRepository;
import com.bakerymanager.repository.SaleItemRepository;
import com.bakerymanager.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@Transactional
public class SaleService {
    
    private static final Logger logger = LoggerFactory.getLogger(SaleService.class);
    
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    
    public SaleService(SaleRepository saleRepository, 
                       SaleItemRepository saleItemRepository,
                       ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
    }
    
    @Transactional
    public Sale createSale(List<CartItem> cartItems, String paymentMethod, BigDecimal cashReceived, String operator) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Coșul este gol");
        }
        
        // Creare vânzare
        Sale sale = new Sale();
        sale.setSaleDate(LocalDateTime.now());
        sale.setPaymentMethod(paymentMethod);
        sale.setCashReceived(cashReceived != null ? cashReceived : BigDecimal.ZERO);
        sale.setOperator(operator != null ? operator : "Operator");
        
        // Calculare total și creare items
        List<SaleItem> saleItems = new ArrayList<>();
        List<Product> productsToUpdate = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (CartItem cartItem : cartItems) {
            Long productId = cartItem.getProductId();
            if (productId == null) {
                throw new IllegalArgumentException("ID-ul produsului nu poate fi null");
            }
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produsul nu există: " + productId));
            
            // Verificare stoc
            if (product.getCurrentStock().compareTo(cartItem.getQuantity()) < 0) {
                throw new IllegalArgumentException("Stoc insuficient pentru: " + product.getName());
            }
            
            // Scădere stoc
            product.setCurrentStock(product.getCurrentStock().subtract(cartItem.getQuantity()));
            productsToUpdate.add(product);
            
            // Creare SaleItem
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setQuantity(cartItem.getQuantity());
            saleItem.setUnitPrice(cartItem.getUnitPrice());
            saleItem.calculateTotal();
            
            saleItems.add(saleItem);
            totalAmount = totalAmount.add(saleItem.getTotalPrice());
        }
        
        sale.setTotalAmount(totalAmount);
        
        // Calculare rest
        if (cashReceived != null && cashReceived.compareTo(totalAmount) > 0) {
            sale.setChangeAmount(cashReceived.subtract(totalAmount));
        } else {
            sale.setChangeAmount(BigDecimal.ZERO);
        }
        
        // Salvare vânzare
        Sale savedSale = saleRepository.save(sale);
        
        // Batch save products and sale items
        productRepository.saveAll(productsToUpdate);
        for (SaleItem item : saleItems) {
            item.setSale(savedSale);
        }
        saleItemRepository.saveAll(saleItems);
        
        logger.info("Sale saved successfully: ID={}, Total={}", savedSale.getId(), savedSale.getTotalAmount());
        return savedSale;
    }
    
    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }
    
    public List<Sale> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.findSalesByDateRange(startDate, endDate);
    }
    
    public List<Sale> getTodaySales() {
        return saleRepository.findTodaySales();
    }
    
    public BigDecimal getTodayTotalSales() {
        return saleRepository.getTodayTotalSales();
    }
    
    public BigDecimal getTotalSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.getTotalSalesByDateRange(startDate, endDate);
    }
    
    public List<SaleItem> getSaleItems(Long saleId) {
        return saleItemRepository.findItemsBySaleId(saleId);
    }
    
    public List<Object[]> getTopSellingProducts(LocalDateTime startDate, LocalDateTime endDate) {
        return saleItemRepository.getTopSellingProductsByDateRange(startDate, endDate);
    }
    
    public Sale getSaleById(Long id) {
        if (id == null) {
            return null;
        }
        return saleRepository.findById(id).orElse(null);
    }
    
    public void deleteSale(Long id) {
        if (id == null) return;
        
        Sale sale = saleRepository.findById(id).orElse(null);
        if (sale != null) {
            // Restaurare stoc
            for (SaleItem item : sale.getSaleItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    product.setCurrentStock(product.getCurrentStock().add(item.getQuantity()));
                    productRepository.save(product);
                }
            }
            
            saleRepository.delete(sale);
            logger.info("Sale deleted: ID={}", id);
        }
    }
    
    // Inner class pentru cart items
    public static class CartItem {
        private Long productId;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        
        public CartItem() {}
        
        public CartItem(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
        
        // Getters and Setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }
}
