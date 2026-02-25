package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a VAT (Tax) summary for compliance reporting.
 * Aggregates tax-related transactions for a date range.
 */
@Entity
@Table(name = "vat_reports", indexes = {
    @Index(name = "idx_vat_period", columnList = "report_start_date, report_end_date")
})
public class VATReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "report_start_date", nullable = false)
    private LocalDate reportStartDate;
    
    @Column(name = "report_end_date", nullable = false)
    private LocalDate reportEndDate;
    
    @Column(name = "period_name", nullable = false)
    private String periodName;  // "Februarie 2026", "Q1 2026", etc.
    
    @Column(name = "taxable_amount", precision = 12, scale = 2)
    private BigDecimal taxableAmount = BigDecimal.ZERO;  // Revenue subject to VAT
    
    @Column(name = "vat_collected", precision = 12, scale = 2)
    private BigDecimal vatCollected = BigDecimal.ZERO;  // VAT on sales
    
    @Column(name = "vat_deductible", precision = 12, scale = 2)
    private BigDecimal vatDeductible = BigDecimal.ZERO;  // VAT on purchases
    
    @Column(name = "vat_payable", precision = 12, scale = 2)
    private BigDecimal vatPayable = BigDecimal.ZERO;  // Collected - Deductible
    
    @Column(name = "vat_0_collected", precision = 12, scale = 2)
    private BigDecimal vat0Collected = BigDecimal.ZERO;  // VAT at 0% rate

    @Column(name = "vat_11_collected", precision = 12, scale = 2)
    private BigDecimal vat11Collected = BigDecimal.ZERO;  // VAT at 11% rate

    @Column(name = "vat_21_collected", precision = 12, scale = 2)
    private BigDecimal vat21Collected = BigDecimal.ZERO;  // VAT at 21% rate
    
    @Column(name = "vat_rate_percent", precision = 5, scale = 2)
    private BigDecimal vatRatePercent = new BigDecimal("19");  // Romania standard
    
    @Column(name = "total_sales", precision = 12, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;
    
    @Column(name = "total_purchases", precision = 12, scale = 2)
    private BigDecimal totalPurchases = BigDecimal.ZERO;
    
    @Column(name = "vat_exempt_sales", precision = 12, scale = 2)
    private BigDecimal vatExemptSales = BigDecimal.ZERO;
    
    @Column(name = "vat_reverse_charge", precision = 12, scale = 2)
    private BigDecimal vatReverseCharge = BigDecimal.ZERO;  // For services
    
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
        recalculateVAT();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalculateVAT();
    }
    
    public void recalculateVAT() {
        // Recalculate total VAT collected from per-rate components
        BigDecimal totalCollected = (vat0Collected != null ? vat0Collected : BigDecimal.ZERO)
            .add(vat11Collected != null ? vat11Collected : BigDecimal.ZERO)
            .add(vat21Collected != null ? vat21Collected : BigDecimal.ZERO);
        
        // If per-rate totals are set, use them; otherwise keep existing vatCollected
        if (totalCollected.compareTo(BigDecimal.ZERO) > 0) {
            vatCollected = totalCollected;
        }
        
        // VAT Payable = VAT Collected - VAT Deductible
        vatPayable = vatCollected.subtract(vatDeductible);
        if (vatPayable.compareTo(BigDecimal.ZERO) < 0) {
            vatPayable = BigDecimal.ZERO;
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDate getReportStartDate() { return reportStartDate; }
    public void setReportStartDate(LocalDate reportStartDate) { this.reportStartDate = reportStartDate; }
    
    public LocalDate getReportEndDate() { return reportEndDate; }
    public void setReportEndDate(LocalDate reportEndDate) { this.reportEndDate = reportEndDate; }
    
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; }
    
    public BigDecimal getVatCollected() { return vatCollected; }
    public void setVatCollected(BigDecimal vatCollected) { this.vatCollected = vatCollected; }
    
    public BigDecimal getVatDeductible() { return vatDeductible; }
    public void setVatDeductible(BigDecimal vatDeductible) { this.vatDeductible = vatDeductible; }
    
    public BigDecimal getVatPayable() { return vatPayable; }
    public void setVatPayable(BigDecimal vatPayable) { this.vatPayable = vatPayable; }
    
    public BigDecimal getVat0Collected() { return vat0Collected; }
    public void setVat0Collected(BigDecimal vat0Collected) { this.vat0Collected = vat0Collected; }

    public BigDecimal getVat11Collected() { return vat11Collected; }
    public void setVat11Collected(BigDecimal vat11Collected) { this.vat11Collected = vat11Collected; }

    public BigDecimal getVat21Collected() { return vat21Collected; }
    public void setVat21Collected(BigDecimal vat21Collected) { this.vat21Collected = vat21Collected; }
    
    public BigDecimal getVatRatePercent() { return vatRatePercent; }
    public void setVatRatePercent(BigDecimal vatRatePercent) { this.vatRatePercent = vatRatePercent; }
    
    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }
    
    public BigDecimal getTotalPurchases() { return totalPurchases; }
    public void setTotalPurchases(BigDecimal totalPurchases) { this.totalPurchases = totalPurchases; }
    
    public BigDecimal getVatExemptSales() { return vatExemptSales; }
    public void setVatExemptSales(BigDecimal vatExemptSales) { this.vatExemptSales = vatExemptSales; }
    
    public BigDecimal getVatReverseCharge() { return vatReverseCharge; }
    public void setVatReverseCharge(BigDecimal vatReverseCharge) { this.vatReverseCharge = vatReverseCharge; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
