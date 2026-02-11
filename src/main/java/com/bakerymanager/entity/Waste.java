package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "waste_tracking")
public class Waste {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;
    
    @Column(name = "item_name", nullable = false)
    private String itemName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType; // PRODUCT or INGREDIENT
    
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "waste_reason", nullable = false)
    private WasteReason reason;
    
    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;
    
    @Column(name = "waste_date", nullable = false)
    private LocalDateTime wasteDate;
    
    @Column(name = "recorded_by")
    private String recordedBy;
    
    @Column(length = 500)
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Enums
    public enum ItemType {
        PRODUCT,
        INGREDIENT
    }
    
    public enum WasteReason {
        EXPIRED,           // Expirat
        DAMAGED,           // Deteriorat
        BURNT,             // Ars
        DROPPED,           // Căzut
        QUALITY_ISSUE,     // Probleme de calitate
        OVERPRODUCTION,    // Supraproducție
        CONTAMINATION,     // Contaminare
        OTHER              // Altele
    }
    
    // Constructor
    public Waste() {
        this.createdAt = LocalDateTime.now();
        this.wasteDate = LocalDateTime.now();
    }
    
    public Waste(Product product, BigDecimal quantity, WasteReason reason) {
        this();
        this.product = product;
        this.itemName = product.getName();
        this.itemType = ItemType.PRODUCT;
        this.quantity = quantity;
        this.reason = reason;
        this.estimatedCost = product.getSalePrice().multiply(quantity);
    }
    
    public Waste(Ingredient ingredient, BigDecimal quantity, WasteReason reason) {
        this();
        this.ingredient = ingredient;
        this.itemName = ingredient.getName();
        this.itemType = ItemType.INGREDIENT;
        this.quantity = quantity;
        this.reason = reason;
        if (ingredient.getLastPurchasePrice() != null) {
            this.estimatedCost = ingredient.getLastPurchasePrice().multiply(quantity);
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.itemName = product.getName();
            this.itemType = ItemType.PRODUCT;
        }
    }
    
    public Ingredient getIngredient() {
        return ingredient;
    }
    
    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
        if (ingredient != null) {
            this.itemName = ingredient.getName();
            this.itemType = ItemType.INGREDIENT;
        }
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public ItemType getItemType() {
        return itemType;
    }
    
    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public WasteReason getReason() {
        return reason;
    }
    
    public void setReason(WasteReason reason) {
        this.reason = reason;
    }
    
    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }
    
    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
    
    public LocalDateTime getWasteDate() {
        return wasteDate;
    }
    
    public void setWasteDate(LocalDateTime wasteDate) {
        this.wasteDate = wasteDate;
    }
    
    public String getRecordedBy() {
        return recordedBy;
    }
    
    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
