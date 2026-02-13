package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ReceptionNote (Notă de Intrare Recepție - NIR)
 * Romanian legal document for receiving goods from suppliers
 */
@Entity
@Table(name = "reception_notes")
public class ReceptionNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Document Information
    @Column(name = "nir_number", unique = true, nullable = false)
    private String nirNumber;
    
    @Column(name = "nir_date", nullable = false)
    private LocalDateTime nirDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NirStatus status = NirStatus.DRAFT;
    
    // Company Information (Beneficiary)
    @Column(name = "company_name", nullable = false)
    private String companyName;
    
    @Column(name = "company_address")
    private String companyAddress;
    
    // Supplier Information (from Invoice)
    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    
    // Reception Details
    @Column(name = "delivery_note_number")
    private String deliveryNoteNumber; // Aviz de însoțire
    
    @Column(name = "reception_date", nullable = false)
    private LocalDateTime receptionDate;
    
    // Lines
    @OneToMany(mappedBy = "receptionNote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReceptionNoteLine> lines = new ArrayList<>();
    
    // Reception Committee (3 members as per Romanian regulations)
    @Column(name = "committee_1_name")
    private String committee1Name;
    
    @Column(name = "committee_1_signature")
    private String committee1Signature;
    
    @Column(name = "committee_2_name")
    private String committee2Name;
    
    @Column(name = "committee_2_signature")
    private String committee2Signature;
    
    @Column(name = "committee_3_name")
    private String committee3Name;
    
    @Column(name = "committee_3_signature")
    private String committee3Signature;
    
    // Warehouse Manager
    @Column(name = "warehouse_manager_name")
    private String warehouseManagerName;
    
    @Column(name = "warehouse_manager_signature")
    private String warehouseManagerSignature;
    
    // Financial Totals
    @Column(name = "total_value_without_vat", precision = 10, scale = 2)
    private BigDecimal totalValueWithoutVAT = BigDecimal.ZERO;
    
    @Column(name = "total_vat", precision = 10, scale = 2)
    private BigDecimal totalVAT = BigDecimal.ZERO;
    
    @Column(name = "total_value", precision = 10, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;
    
    // Discrepancies
    @Column(name = "has_discrepancies")
    private Boolean hasDiscrepancies = false;
    
    @Column(name = "discrepancies_notes", length = 1000)
    private String discrepanciesNotes;
    
    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (nirDate == null) {
            nirDate = LocalDateTime.now();
        }
        if (receptionDate == null) {
            receptionDate = LocalDateTime.now();
        }
        if (nirNumber == null) {
            generateNirNumber();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Business Methods
    
    public void generateNirNumber() {
        // Format: NIR-YYYYMMDD-XXXX
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", (int)(Math.random() * 10000));
        this.nirNumber = "NIR-" + dateStr + "-" + random;
    }
    
    public void calculateTotals() {
        BigDecimal sumWithoutVAT = BigDecimal.ZERO;
        BigDecimal sumVAT = BigDecimal.ZERO;
        BigDecimal sumTotal = BigDecimal.ZERO;
        
        for (ReceptionNoteLine line : lines) {
            line.calculateValues();
            sumWithoutVAT = sumWithoutVAT.add(line.getValueWithoutVAT());
            sumVAT = sumVAT.add(line.getVatAmount());
            sumTotal = sumTotal.add(line.getTotalValue());
        }
        
        this.totalValueWithoutVAT = sumWithoutVAT;
        this.totalVAT = sumVAT;
        this.totalValue = sumTotal;
    }
    
    public void checkDiscrepancies() {
        boolean hasAnyDiscrepancy = false;
        for (ReceptionNoteLine line : lines) {
            if (line.getHasDiscrepancy()) {
                hasAnyDiscrepancy = true;
                break;
            }
        }
        this.hasDiscrepancies = hasAnyDiscrepancy;
    }
    
    public boolean canApprove() {
        return status == NirStatus.DRAFT && 
               committee1Name != null && 
               committee2Name != null && 
               committee3Name != null;
    }
    
    public boolean canSign() {
        return status == NirStatus.APPROVED && 
               warehouseManagerName != null;
    }
    
    public void addLine(ReceptionNoteLine line) {
        lines.add(line);
        line.setReceptionNote(this);
    }
    
    public void removeLine(ReceptionNoteLine line) {
        lines.remove(line);
        line.setReceptionNote(null);
    }
    
    // Enum for NIR Status
    public enum NirStatus {
        DRAFT("Ciornă"),
        APPROVED("Aprobat"),
        SIGNED("Semnat");
        
        private final String displayName;
        
        NirStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNirNumber() {
        return nirNumber;
    }
    
    public void setNirNumber(String nirNumber) {
        this.nirNumber = nirNumber;
    }
    
    public LocalDateTime getNirDate() {
        return nirDate;
    }
    
    public void setNirDate(LocalDateTime nirDate) {
        this.nirDate = nirDate;
    }
    
    public NirStatus getStatus() {
        return status;
    }
    
    public void setStatus(NirStatus status) {
        this.status = status;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getCompanyAddress() {
        return companyAddress;
    }
    
    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }
    
    public Invoice getInvoice() {
        return invoice;
    }
    
    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }
    
    public String getDeliveryNoteNumber() {
        return deliveryNoteNumber;
    }
    
    public void setDeliveryNoteNumber(String deliveryNoteNumber) {
        this.deliveryNoteNumber = deliveryNoteNumber;
    }
    
    public LocalDateTime getReceptionDate() {
        return receptionDate;
    }
    
    public void setReceptionDate(LocalDateTime receptionDate) {
        this.receptionDate = receptionDate;
    }
    
    public List<ReceptionNoteLine> getLines() {
        return lines;
    }
    
    public void setLines(List<ReceptionNoteLine> lines) {
        this.lines = lines;
    }
    
    public String getCommittee1Name() {
        return committee1Name;
    }
    
    public void setCommittee1Name(String committee1Name) {
        this.committee1Name = committee1Name;
    }
    
    public String getCommittee1Signature() {
        return committee1Signature;
    }
    
    public void setCommittee1Signature(String committee1Signature) {
        this.committee1Signature = committee1Signature;
    }
    
    public String getCommittee2Name() {
        return committee2Name;
    }
    
    public void setCommittee2Name(String committee2Name) {
        this.committee2Name = committee2Name;
    }
    
    public String getCommittee2Signature() {
        return committee2Signature;
    }
    
    public void setCommittee2Signature(String committee2Signature) {
        this.committee2Signature = committee2Signature;
    }
    
    public String getCommittee3Name() {
        return committee3Name;
    }
    
    public void setCommittee3Name(String committee3Name) {
        this.committee3Name = committee3Name;
    }
    
    public String getCommittee3Signature() {
        return committee3Signature;
    }
    
    public void setCommittee3Signature(String committee3Signature) {
        this.committee3Signature = committee3Signature;
    }
    
    public String getWarehouseManagerName() {
        return warehouseManagerName;
    }
    
    public void setWarehouseManagerName(String warehouseManagerName) {
        this.warehouseManagerName = warehouseManagerName;
    }
    
    public String getWarehouseManagerSignature() {
        return warehouseManagerSignature;
    }
    
    public void setWarehouseManagerSignature(String warehouseManagerSignature) {
        this.warehouseManagerSignature = warehouseManagerSignature;
    }
    
    public BigDecimal getTotalValueWithoutVAT() {
        return totalValueWithoutVAT;
    }
    
    public void setTotalValueWithoutVAT(BigDecimal totalValueWithoutVAT) {
        this.totalValueWithoutVAT = totalValueWithoutVAT;
    }
    
    public BigDecimal getTotalVAT() {
        return totalVAT;
    }
    
    public void setTotalVAT(BigDecimal totalVAT) {
        this.totalVAT = totalVAT;
    }
    
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    public Boolean getHasDiscrepancies() {
        return hasDiscrepancies;
    }
    
    public void setHasDiscrepancies(Boolean hasDiscrepancies) {
        this.hasDiscrepancies = hasDiscrepancies;
    }
    
    public String getDiscrepanciesNotes() {
        return discrepanciesNotes;
    }
    
    public void setDiscrepanciesNotes(String discrepanciesNotes) {
        this.discrepanciesNotes = discrepanciesNotes;
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
