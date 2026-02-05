package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;
    
    @Column(name = "supplier_name", nullable = false)
    private String supplierName;
    
    @Column(name = "supplier_cui")
    private String supplierCui;
    
    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;
    
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "is_spv_imported")
    private Boolean isSpvImported = false;
    
    @Column(name = "xml_file_path")
    private String xmlFilePath;
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceLine> invoiceLines;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isSpvImported == null) {
            isSpvImported = false;
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void calculateTotal() {
        if (invoiceLines != null) {
            totalAmount = invoiceLines.stream()
                .map(InvoiceLine::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    
    public String getSupplierCui() { return supplierCui; }
    public void setSupplierCui(String supplierCui) { this.supplierCui = supplierCui; }
    
    public LocalDateTime getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public Boolean getIsSpvImported() { return isSpvImported; }
    public void setIsSpvImported(Boolean isSpvImported) { this.isSpvImported = isSpvImported; }
    
    public String getXmlFilePath() { return xmlFilePath; }
    public void setXmlFilePath(String xmlFilePath) { this.xmlFilePath = xmlFilePath; }
    
    public List<InvoiceLine> getInvoiceLines() { return invoiceLines; }
    public void setInvoiceLines(List<InvoiceLine> invoiceLines) { this.invoiceLines = invoiceLines; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
