package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a cost breakdown for a product.
 * Aggregates raw material costs, labor, and overhead.
 */
@Entity
@Table(name = "cost_breakdowns", indexes = {
    @Index(name = "idx_cost_product_date", columnList = "product_id, cost_date"),
    @Index(name = "idx_cost_date", columnList = "cost_date")
})
public class CostBreakdown {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "cost_date", nullable = false)
    private LocalDate costDate;
    
    @Column(name = "raw_material_cost", precision = 10, scale = 4)
    private BigDecimal rawMaterialCost = BigDecimal.ZERO;  // From FEFO consumption
    
    @Column(name = "labor_cost", precision = 10, scale = 4)
    private BigDecimal laborCost = BigDecimal.ZERO;  // From shift duration
    
    @Column(name = "overhead_cost", precision = 10, scale = 4)
    private BigDecimal overheadCost = BigDecimal.ZERO;  // Fixed allocation
    
    @Column(name = "semifabricat_cost", precision = 10, scale = 4)
    private BigDecimal semifabricatCost = BigDecimal.ZERO;  // From multi-level recipes
    
    @Column(name = "total_cost", precision = 10, scale = 4)
    private BigDecimal totalCost = BigDecimal.ZERO;
    
    @Column(name = "unit_cost", precision = 10, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;  // Total / quantity produced
    
    @Column(name = "quantity_produced", precision = 10, scale = 3)
    private BigDecimal quantityProduced = BigDecimal.ZERO;
    
    @Column(name = "cost_method", nullable = false)
    private String costMethod = "FEFO";  // FEFO, STANDARD, AVERAGE
    
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
        if (costMethod == null) {
            costMethod = "FEFO";
        }
        recalculateTotal();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalculateTotal();
    }
    
    public void recalculateTotal() {
        totalCost = rawMaterialCost.add(laborCost).add(overheadCost).add(semifabricatCost);
        if (quantityProduced != null && quantityProduced.compareTo(BigDecimal.ZERO) > 0) {
            unitCost = totalCost.divide(quantityProduced, 4, java.math.RoundingMode.HALF_UP);
        } else {
            unitCost = BigDecimal.ZERO;
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public LocalDate getCostDate() { return costDate; }
    public void setCostDate(LocalDate costDate) { this.costDate = costDate; }
    
    public BigDecimal getRawMaterialCost() { return rawMaterialCost; }
    public void setRawMaterialCost(BigDecimal rawMaterialCost) { this.rawMaterialCost = rawMaterialCost; }
    
    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }
    
    public BigDecimal getOverheadCost() { return overheadCost; }
    public void setOverheadCost(BigDecimal overheadCost) { this.overheadCost = overheadCost; }
    
    public BigDecimal getSemifabricatCost() { return semifabricatCost; }
    public void setSemifabricatCost(BigDecimal semifabricatCost) { this.semifabricatCost = semifabricatCost; }
    
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(BigDecimal quantityProduced) { this.quantityProduced = quantityProduced; }
    
    public String getCostMethod() { return costMethod; }
    public void setCostMethod(String costMethod) { this.costMethod = costMethod; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
