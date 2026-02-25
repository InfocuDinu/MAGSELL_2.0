package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a production shift with time windows and resource assignments.
 * Tracks shifts for production scheduling and capacity management.
 */
@Entity
@Table(name = "production_shifts", indexes = {
    @Index(name = "idx_shift_date_range", columnList = "shift_start, shift_end"),
    @Index(name = "idx_shift_resource_type", columnList = "resource_type")
})
public class ProductionShift {
    
    public enum ResourceType {
        OVEN("Cuptor", "OVEN"),
        LINE("Linie producție", "LINE"),
        PERSONNEL("Personal", "PERSONNEL"),
        MIXING("Amestecare", "MIXING");
        
        private final String displayName;
        private final String code;
        
        ResourceType(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    public enum ShiftStatus {
        AVAILABLE("Disponibil"),
        IN_PROGRESS("În lucru"),
        COMPLETED("Finalizat"),
        BLOCKED("Blocat");
        
        private final String displayName;
        
        ShiftStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "shift_name", nullable = false)
    private String shiftName;
    
    @Column(name = "shift_start", nullable = false)
    private LocalDateTime shiftStart;
    
    @Column(name = "shift_end", nullable = false)
    private LocalDateTime shiftEnd;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;
    
    @Column(name = "resource_id")
    private String resourceId;  // e.g., "OVEN_1", "LINE_A", "PERSONNEL_JOHN"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShiftStatus status = ShiftStatus.AVAILABLE;
    
    @Column(name = "capacity_units", precision = 10, scale = 2)
    private java.math.BigDecimal capacityUnits;  // Total capacity for this shift
    
    @Column(name = "allocated_units", precision = 10, scale = 2)
    private java.math.BigDecimal allocatedUnits = java.math.BigDecimal.ZERO;
    
    @Column(name = "notes", length = 500)
    private String notes;
    
    @OneToMany(mappedBy = "productionShift", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionScheduleEntry> scheduleEntries = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (allocatedUnits == null) {
            allocatedUnits = java.math.BigDecimal.ZERO;
        }
        if (status == null) {
            status = ShiftStatus.AVAILABLE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public java.math.BigDecimal getAvailableCapacity() {
        if (capacityUnits == null) {
            return java.math.BigDecimal.ZERO;
        }
        return capacityUnits.subtract(allocatedUnits);
    }
    
    public boolean canAllocate(java.math.BigDecimal requiredCapacity) {
        return getAvailableCapacity().compareTo(requiredCapacity) >= 0;
    }
    
    public void allocateCapacity(java.math.BigDecimal quantity) {
        if (quantity != null && quantity.compareTo(java.math.BigDecimal.ZERO) > 0) {
            allocatedUnits = allocatedUnits.add(quantity);
        }
    }
    
    public void deallocateCapacity(java.math.BigDecimal quantity) {
        if (quantity != null && quantity.compareTo(java.math.BigDecimal.ZERO) > 0) {
            allocatedUnits = allocatedUnits.subtract(quantity);
            if (allocatedUnits.compareTo(java.math.BigDecimal.ZERO) < 0) {
                allocatedUnits = java.math.BigDecimal.ZERO;
            }
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }
    
    public LocalDateTime getShiftStart() { return shiftStart; }
    public void setShiftStart(LocalDateTime shiftStart) { this.shiftStart = shiftStart; }
    
    public LocalDateTime getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(LocalDateTime shiftEnd) { this.shiftEnd = shiftEnd; }
    
    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }
    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public ShiftStatus getStatus() { return status; }
    public void setStatus(ShiftStatus status) { this.status = status; }
    
    public java.math.BigDecimal getCapacityUnits() { return capacityUnits; }
    public void setCapacityUnits(java.math.BigDecimal capacityUnits) { this.capacityUnits = capacityUnits; }
    
    public java.math.BigDecimal getAllocatedUnits() { return allocatedUnits; }
    public void setAllocatedUnits(java.math.BigDecimal allocatedUnits) { this.allocatedUnits = allocatedUnits; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public List<ProductionScheduleEntry> getScheduleEntries() { return scheduleEntries; }
    public void setScheduleEntries(List<ProductionScheduleEntry> scheduleEntries) { this.scheduleEntries = scheduleEntries; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
