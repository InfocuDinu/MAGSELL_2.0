package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a scheduled production entry.
 * Links a production order line to a shift with allocation details.
 */
@Entity
@Table(name = "production_schedule_entries", indexes = {
    @Index(name = "idx_entry_shift_id", columnList = "shift_id"),
    @Index(name = "idx_entry_order_line_id", columnList = "order_line_id")
})
public class ProductionScheduleEntry {
    
    public enum ScheduleEntryStatus {
        SCHEDULED("Planificat"),
        CONFIRMED("Confirmat"),
        IN_PROGRESS("În lucru"),
        COMPLETED("Finalizat"),
        CANCELLED("Anulat");
        
        private final String displayName;
        
        ScheduleEntryStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private ProductionShift productionShift;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_id", nullable = false)
    private ProductionOrderLine productionOrderLine;
    
    @Column(name = "allocated_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal allocatedQuantity;
    
    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 5;  // 1-10, higher = more urgent
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScheduleEntryStatus status = ScheduleEntryStatus.SCHEDULED;
    
    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;
    
    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;
    
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
        if (status == null) {
            status = ScheduleEntryStatus.SCHEDULED;
        }
        if (priority == null) {
            priority = 5;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ProductionShift getProductionShift() { return productionShift; }
    public void setProductionShift(ProductionShift productionShift) { this.productionShift = productionShift; }
    
    public ProductionOrderLine getProductionOrderLine() { return productionOrderLine; }
    public void setProductionOrderLine(ProductionOrderLine productionOrderLine) { this.productionOrderLine = productionOrderLine; }
    
    public BigDecimal getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(BigDecimal allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }
    
    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public ScheduleEntryStatus getStatus() { return status; }
    public void setStatus(ScheduleEntryStatus status) { this.status = status; }
    
    public LocalDateTime getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(LocalDateTime actualStartTime) { this.actualStartTime = actualStartTime; }
    
    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
