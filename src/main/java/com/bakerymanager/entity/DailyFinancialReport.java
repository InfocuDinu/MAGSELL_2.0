package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a daily financial summary.
 * Aggregates revenue, COGS, and profit for a specific date.
 */
@Entity
@Table(name = "daily_financial_reports", indexes = {
    @Index(name = "idx_daily_date", columnList = "report_date")
})
public class DailyFinancialReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;
    
    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;  // Sum of sales
    
    @Column(name = "total_cogs", precision = 12, scale = 2)
    private BigDecimal totalCOGS = BigDecimal.ZERO;  // Cost of goods sold
    
    @Column(name = "gross_profit", precision = 12, scale = 2)
    private BigDecimal grossProfit = BigDecimal.ZERO;  // Revenue - COGS
    
    @Column(name = "gross_margin_percent", precision = 5, scale = 2)
    private BigDecimal grossMarginPercent = BigDecimal.ZERO;  // (Profit / Revenue) * 100
    
    @Column(name = "labor_cost", precision = 12, scale = 2)
    private BigDecimal laborCost = BigDecimal.ZERO;  // From shifts
    
    @Column(name = "overhead_cost", precision = 12, scale = 2)
    private BigDecimal overheadCost = BigDecimal.ZERO;  // Fixed allocation
    
    @Column(name = "operating_profit", precision = 12, scale = 2)
    private BigDecimal operatingProfit = BigDecimal.ZERO;  // Gross profit - labor - overhead
    
    @Column(name = "operating_margin_percent", precision = 5, scale = 2)
    private BigDecimal operatingMarginPercent = BigDecimal.ZERO;
    
    @Column(name = "vat_0_percent", precision = 12, scale = 2)
    private BigDecimal vat0Collected = BigDecimal.ZERO;  // VAT at 0% rate
    
    @Column(name = "vat_11_percent", precision = 12, scale = 2)
    private BigDecimal vat11Collected = BigDecimal.ZERO;  // VAT at 11% rate
    
    @Column(name = "vat_21_percent", precision = 12, scale = 2)
    private BigDecimal vat21Collected = BigDecimal.ZERO;  // VAT at 21% rate
    
    @Column(name = "total_vat", precision = 12, scale = 2)
    private BigDecimal totalVAT = BigDecimal.ZERO;  // Total VAT collected
    
    @Column(name = "units_produced")
    private Integer unitsProduced = 0;
    
    @Column(name = "units_sold")
    private Integer unitsSold = 0;
    
    @Column(name = "waste_value", precision = 12, scale = 2)
    private BigDecimal wasteValue = BigDecimal.ZERO;
    
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
        recalculateMetrics();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalculateMetrics();
    }
    
    public void recalculateMetrics() {
        // Recalculate total VAT from per-rate components
        totalVAT = (vat0Collected != null ? vat0Collected : BigDecimal.ZERO)
            .add(vat11Collected != null ? vat11Collected : BigDecimal.ZERO)
            .add(vat21Collected != null ? vat21Collected : BigDecimal.ZERO);
        
        // Calculate gross profit
        grossProfit = totalRevenue.subtract(totalCOGS);
        
        // Calculate gross margin %
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            grossMarginPercent = grossProfit.divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        } else {
            grossMarginPercent = BigDecimal.ZERO;
        }
        
        // Calculate operating profit
        operatingProfit = grossProfit.subtract(laborCost).subtract(overheadCost);
        
        // Calculate operating margin %
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            operatingMarginPercent = operatingProfit.divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        } else {
            operatingMarginPercent = BigDecimal.ZERO;
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public BigDecimal getTotalCOGS() { return totalCOGS; }
    public void setTotalCOGS(BigDecimal totalCOGS) { this.totalCOGS = totalCOGS; }
    
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
    
    public BigDecimal getGrossMarginPercent() { return grossMarginPercent; }
    public void setGrossMarginPercent(BigDecimal grossMarginPercent) { this.grossMarginPercent = grossMarginPercent; }
    
    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }
    
    public BigDecimal getOverheadCost() { return overheadCost; }
    public void setOverheadCost(BigDecimal overheadCost) { this.overheadCost = overheadCost; }
    
    public BigDecimal getOperatingProfit() { return operatingProfit; }
    public void setOperatingProfit(BigDecimal operatingProfit) { this.operatingProfit = operatingProfit; }
    
    public BigDecimal getOperatingMarginPercent() { return operatingMarginPercent; }
    public void setOperatingMarginPercent(BigDecimal operatingMarginPercent) { this.operatingMarginPercent = operatingMarginPercent; }
    
    public BigDecimal getVat0Collected() { return vat0Collected; }
    public void setVat0Collected(BigDecimal vat0Collected) { this.vat0Collected = vat0Collected; }

    public BigDecimal getVat11Collected() { return vat11Collected; }
    public void setVat11Collected(BigDecimal vat11Collected) { this.vat11Collected = vat11Collected; }

    public BigDecimal getVat21Collected() { return vat21Collected; }
    public void setVat21Collected(BigDecimal vat21Collected) { this.vat21Collected = vat21Collected; }
    
    public BigDecimal getTotalVAT() { return totalVAT; }
    public void setTotalVAT(BigDecimal totalVAT) { this.totalVAT = totalVAT; }
    
    public Integer getUnitsProduced() { return unitsProduced; }
    public void setUnitsProduced(Integer unitsProduced) { this.unitsProduced = unitsProduced; }
    
    public Integer getUnitsSold() { return unitsSold; }
    public void setUnitsSold(Integer unitsSold) { this.unitsSold = unitsSold; }
    
    public BigDecimal getWasteValue() { return wasteValue; }
    public void setWasteValue(BigDecimal wasteValue) { this.wasteValue = wasteValue; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
