package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(name = "sale_price", nullable = false, precision = 8, scale = 2)
    private BigDecimal salePrice;
    
    @Column(name = "physical_stock", nullable = false, precision = 10, scale = 3)
    private BigDecimal physicalStock;
    
    @Column(name = "minimum_stock", precision = 10, scale = 3)
    private BigDecimal minimumStock;
    
    @Column(name = "barcode")
    private String barcode;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // NEW: Expiration date tracking for food safety
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    // NEW: Batch/lot tracking for traceability
    @Column(name = "batch_number")
    private String batchNumber;
    
    @Column(name = "production_date")
    private LocalDate productionDate;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RecipeItem> recipeItems;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (physicalStock == null) {
            physicalStock = BigDecimal.ZERO;
        }
        if (minimumStock == null) {
            minimumStock = BigDecimal.ZERO;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void addStock(BigDecimal quantity) {
        this.physicalStock = this.physicalStock.add(quantity);
    }
    
    public void removeStock(BigDecimal quantity) {
        this.physicalStock = this.physicalStock.subtract(quantity);
    }
    
    public boolean hasSufficientStock(BigDecimal requiredQuantity) {
        return this.physicalStock.compareTo(requiredQuantity) >= 0;
    }
    
    public BigDecimal getCurrentStock() {
        return physicalStock;
    }
    
    public void setCurrentStock(BigDecimal stock) {
        this.physicalStock = stock;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    
    public BigDecimal getPhysicalStock() { return physicalStock; }
    public void setPhysicalStock(BigDecimal physicalStock) { this.physicalStock = physicalStock; }
    
    public BigDecimal getMinimumStock() { return minimumStock; }
    public void setMinimumStock(BigDecimal minimumStock) { this.minimumStock = minimumStock; }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public List<RecipeItem> getRecipeItems() { return recipeItems; }
    public void setRecipeItems(List<RecipeItem> recipeItems) { this.recipeItems = recipeItems; }
    
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Helper method to check if product is expired
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }
    
    // Helper method to check if product is expiring soon (within 3 days for bakery products)
    public boolean isExpiringSoon() {
        if (expirationDate == null) return false;
        LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);
        return expirationDate.isAfter(LocalDate.now()) && expirationDate.isBefore(threeDaysFromNow);
    }
}
