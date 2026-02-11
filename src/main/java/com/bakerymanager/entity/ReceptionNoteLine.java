package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ReceptionNoteLine - Individual line item in a Reception Note (NIR)
 */
@Entity
@Table(name = "reception_note_lines")
public class ReceptionNoteLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "reception_note_id", nullable = false)
    private ReceptionNote receptionNote;
    
    // Product Information
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "product_code")
    private String productCode;
    
    @Column(name = "unit", nullable = false)
    private String unit; // UM (unitate de măsură): buc, kg, L, etc.
    
    // Quantities
    @Column(name = "invoiced_quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal invoicedQuantity; // Cantitate facturată
    
    @Column(name = "received_quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal receivedQuantity; // Cantitate recepționată
    
    @Column(name = "quantity_difference", precision = 10, scale = 3)
    private BigDecimal quantityDifference; // Diferență
    
    // Pricing
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice; // Preț unitar
    
    @Column(name = "value_without_vat", precision = 10, scale = 2)
    private BigDecimal valueWithoutVAT; // Valoare fără TVA
    
    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.valueOf(19); // Cota TVA (default 19%)
    
    @Column(name = "vat_amount", precision = 10, scale = 2)
    private BigDecimal vatAmount; // TVA
    
    @Column(name = "total_value", precision = 10, scale = 2)
    private BigDecimal totalValue; // Valoare totală
    
    // Discrepancy
    @Column(name = "has_discrepancy")
    private Boolean hasDiscrepancy = false;
    
    @Column(name = "discrepancy_notes")
    private String discrepancyNotes;
    
    @PrePersist
    @PreUpdate
    protected void onSave() {
        calculateValues();
        calculateDifference();
    }
    
    // Business Methods
    
    public void calculateValues() {
        if (receivedQuantity != null && unitPrice != null) {
            // Value without VAT = received quantity * unit price
            valueWithoutVAT = receivedQuantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            
            // VAT amount = value without VAT * (VAT rate / 100)
            if (vatRate != null) {
                vatAmount = valueWithoutVAT.multiply(vatRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);
            } else {
                vatAmount = BigDecimal.ZERO;
            }
            
            // Total value = value without VAT + VAT amount
            totalValue = valueWithoutVAT.add(vatAmount).setScale(2, RoundingMode.HALF_UP);
        }
    }
    
    public void calculateDifference() {
        if (invoicedQuantity != null && receivedQuantity != null) {
            quantityDifference = receivedQuantity.subtract(invoicedQuantity);
            hasDiscrepancy = quantityDifference.compareTo(BigDecimal.ZERO) != 0;
        }
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ReceptionNote getReceptionNote() {
        return receptionNote;
    }
    
    public void setReceptionNote(ReceptionNote receptionNote) {
        this.receptionNote = receptionNote;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductCode() {
        return productCode;
    }
    
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public BigDecimal getInvoicedQuantity() {
        return invoicedQuantity;
    }
    
    public void setInvoicedQuantity(BigDecimal invoicedQuantity) {
        this.invoicedQuantity = invoicedQuantity;
        calculateDifference();
    }
    
    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }
    
    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
        calculateDifference();
        calculateValues();
    }
    
    public BigDecimal getQuantityDifference() {
        return quantityDifference;
    }
    
    public void setQuantityDifference(BigDecimal quantityDifference) {
        this.quantityDifference = quantityDifference;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateValues();
    }
    
    public BigDecimal getValueWithoutVAT() {
        return valueWithoutVAT;
    }
    
    public void setValueWithoutVAT(BigDecimal valueWithoutVAT) {
        this.valueWithoutVAT = valueWithoutVAT;
    }
    
    public BigDecimal getVatRate() {
        return vatRate;
    }
    
    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
        calculateValues();
    }
    
    public BigDecimal getVatAmount() {
        return vatAmount;
    }
    
    public void setVatAmount(BigDecimal vatAmount) {
        this.vatAmount = vatAmount;
    }
    
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    public Boolean getHasDiscrepancy() {
        return hasDiscrepancy;
    }
    
    public void setHasDiscrepancy(Boolean hasDiscrepancy) {
        this.hasDiscrepancy = hasDiscrepancy;
    }
    
    public String getDiscrepancyNotes() {
        return discrepancyNotes;
    }
    
    public void setDiscrepancyNotes(String discrepancyNotes) {
        this.discrepancyNotes = discrepancyNotes;
    }
}
