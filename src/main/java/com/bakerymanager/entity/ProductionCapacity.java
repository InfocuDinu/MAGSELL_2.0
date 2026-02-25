package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents production capacity constraints.
 * Tracks limits for ovens, production lines, personnel, etc.
 */
@Entity
@Table(name = "production_capacities", indexes = {
    @Index(name = "idx_capacity_resource_type", columnList = "resource_type"),
    @Index(name = "idx_capacity_date", columnList = "capacity_date")
})
public class ProductionCapacity {
    
    public enum CapacityUnit {
        KILOGRAMS("Kilograme", "KG"),
        UNITS("Bucăți", "BUC"),
        HOURS("Ore", "ORE"),
        HOURS_PERCENTAGE("% Ore disponibile", "PERCENT_HOURS");
        
        private final String displayName;
        private final String code;
        
        CapacityUnit(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }
        
        public String getDisplayName() { return displayName; }
        public String getCode() { return code; }
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "resource_name", nullable = false)
    private String resourceName;  // "Cuptor 1", "Linie A", "Personal"
    
    @Column(name = "resource_type", nullable = false)
    private String resourceType;  // "OVEN", "LINE", "PERSONNEL"
    
    @Column(name = "capacity_date", nullable = false)
    private java.time.LocalDate capacityDate;
    
    @Column(name = "max_capacity", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxCapacity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "capacity_unit", nullable = false)
    private CapacityUnit capacityUnit;
    
    @Column(name = "current_usage", precision = 10, scale = 2)
    private BigDecimal currentUsage = BigDecimal.ZERO;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "notes", length = 500)
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentUsage == null) {
            currentUsage = BigDecimal.ZERO;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getAvailableCapacity() {
        if (maxCapacity == null) {
            return BigDecimal.ZERO;
        }
        return maxCapacity.subtract(currentUsage);
    }
    
    public boolean canAllocate(BigDecimal requiredCapacity) {
        return getAvailableCapacity().compareTo(requiredCapacity) >= 0;
    }
    
    public void addUsage(BigDecimal quantity) {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
            currentUsage = currentUsage.add(quantity);
        }
    }
    
    public void removeUsage(BigDecimal quantity) {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
            currentUsage = currentUsage.subtract(quantity);
            if (currentUsage.compareTo(BigDecimal.ZERO) < 0) {
                currentUsage = BigDecimal.ZERO;
            }
        }
    }
    
    public double getUsagePercentage() {
        if (maxCapacity == null || maxCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return currentUsage.doubleValue() / maxCapacity.doubleValue() * 100.0;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    
    public java.time.LocalDate getCapacityDate() { return capacityDate; }
    public void setCapacityDate(java.time.LocalDate capacityDate) { this.capacityDate = capacityDate; }
    
    public BigDecimal getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(BigDecimal maxCapacity) { this.maxCapacity = maxCapacity; }
    
    public CapacityUnit getCapacityUnit() { return capacityUnit; }
    public void setCapacityUnit(CapacityUnit capacityUnit) { this.capacityUnit = capacityUnit; }
    
    public BigDecimal getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(BigDecimal currentUsage) { this.currentUsage = currentUsage; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
