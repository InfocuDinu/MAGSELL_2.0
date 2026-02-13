package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_orders")
public class CustomOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;
    
    @Column(name = "customization", length = 1000)
    private String customization;
    
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 8, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(name = "advance_payment", precision = 10, scale = 2)
    private BigDecimal advancePayment = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;
    
    @Column(name = "completion_date")
    private LocalDateTime completionDate;
    
    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;
    
    @Column(length = 500)
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Order status enum
    public enum OrderStatus {
        PENDING,      // Comandă plasată, în așteptare
        CONFIRMED,    // Comandă confirmată
        IN_PROGRESS,  // În producție
        READY,        // Gata pentru livrare
        DELIVERED,    // Livrată clientului
        CANCELLED     // Anulată
    }
    
    // Constructors
    public CustomOrder() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
        this.advancePayment = BigDecimal.ZERO;
    }
    
    public CustomOrder(Customer customer, String productName, BigDecimal quantity, BigDecimal unitPrice, LocalDateTime dueDate) {
        this();
        this.customer = customer;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(quantity);
        this.dueDate = dueDate;
    }
    
    // Business methods
    public BigDecimal getRemainingPayment() {
        return totalPrice.subtract(advancePayment);
    }
    
    public boolean isOverdue() {
        return status != OrderStatus.DELIVERED && 
               status != OrderStatus.CANCELLED && 
               LocalDateTime.now().isAfter(dueDate);
    }
    
    public void addAdvancePayment(BigDecimal amount) {
        this.advancePayment = this.advancePayment.add(amount);
        if (this.advancePayment.compareTo(this.totalPrice) >= 0) {
            this.advancePayment = this.totalPrice;
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        
        if (newStatus == OrderStatus.READY && this.completionDate == null) {
            this.completionDate = LocalDateTime.now();
        } else if (newStatus == OrderStatus.DELIVERED && this.deliveryDate == null) {
            this.deliveryDate = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getCustomization() {
        return customization;
    }
    
    public void setCustomization(String customization) {
        this.customization = customization;
        this.updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        this.totalPrice = this.unitPrice.multiply(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(this.quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
        this.updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getAdvancePayment() {
        return advancePayment;
    }
    
    public void setAdvancePayment(BigDecimal advancePayment) {
        this.advancePayment = advancePayment;
        this.updatedAt = LocalDateTime.now();
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCompletionDate() {
        return completionDate;
    }
    
    public void setCompletionDate(LocalDateTime completionDate) {
        this.completionDate = completionDate;
    }
    
    public LocalDateTime getDeliveryDate() {
        return deliveryDate;
    }
    
    public void setDeliveryDate(LocalDateTime deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
