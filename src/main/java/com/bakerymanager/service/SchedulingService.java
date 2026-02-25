package com.bakerymanager.service;

import com.bakerymanager.entity.*;
import com.bakerymanager.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for production scheduling with capacity management and conflict detection.
 */
@Service
@Transactional
public class SchedulingService {
    
    private final ProductionShiftRepository shiftRepository;
    private final ProductionCapacityRepository capacityRepository;
    private final ProductionScheduleEntryRepository scheduleEntryRepository;
    private final ProductionOrderLineRepository orderLineRepository;
    private final ProductService productService;
    
    public SchedulingService(ProductionShiftRepository shiftRepository,
                            ProductionCapacityRepository capacityRepository,
                            ProductionScheduleEntryRepository scheduleEntryRepository,
                            ProductionOrderLineRepository orderLineRepository,
                            ProductService productService) {
        this.shiftRepository = shiftRepository;
        this.capacityRepository = capacityRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.orderLineRepository = orderLineRepository;
        this.productService = productService;
    }
    
    /**
     * Create a new production shift for resource allocation.
     */
    public ProductionShift createShift(String shiftName,
                                      LocalDateTime shiftStart,
                                      LocalDateTime shiftEnd,
                                      ProductionShift.ResourceType resourceType,
                                      String resourceId,
                                      BigDecimal capacityUnits) {
        if (shiftStart.isAfter(shiftEnd) || shiftStart.equals(shiftEnd)) {
            throw new RuntimeException("Shift start time must be before end time");
        }
        if (capacityUnits != null && capacityUnits.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Capacity units must be greater than 0");
        }
        
        ProductionShift shift = new ProductionShift();
        shift.setShiftName(shiftName);
        shift.setShiftStart(shiftStart);
        shift.setShiftEnd(shiftEnd);
        shift.setResourceType(resourceType);
        shift.setResourceId(resourceId);
        shift.setCapacityUnits(capacityUnits != null ? capacityUnits : BigDecimal.ZERO);
        shift.setStatus(ProductionShift.ShiftStatus.AVAILABLE);
        
        return shiftRepository.save(shift);
    }
    
    /**
     * Find shifts with available capacity for scheduling.
     */
    public List<ProductionShift> findAvailableShifts(LocalDateTime start,
                                                    LocalDateTime end,
                                                    ProductionShift.ResourceType resourceType) {
        return shiftRepository.findShiftsWithAvailableCapacity(resourceType, start, end);
    }
    
    /**
     * Schedule a production order line to a specific shift with conflict detection.
     */
    public ProductionScheduleEntry scheduleOrderToShift(Long orderLineId,
                                                        Long shiftId,
                                                        BigDecimal allocatedQuantity,
                                                        Integer priority) {
        ProductionOrderLine orderLine = orderLineRepository.findById(orderLineId)
            .orElseThrow(() -> new RuntimeException("Production order line not found: " + orderLineId));
        
        ProductionShift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new RuntimeException("Production shift not found: " + shiftId));
        
        if (allocatedQuantity == null || allocatedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Allocated quantity must be greater than 0");
        }
        
        // Check if order line already scheduled
        if (scheduleEntryRepository.isOrderLineScheduled(orderLineId)) {
            throw new RuntimeException("Production order line is already scheduled");
        }
        
        // Calculate estimated duration based on product tech sheet
        int estimatedDurationMinutes = calculateEstimatedDuration(orderLine, allocatedQuantity);
        
        // Validate shift capacity and time window
        if (!shift.canAllocate(allocatedQuantity)) {
            throw new RuntimeException("Insufficient capacity in shift: Available=" + 
                shift.getAvailableCapacity() + ", Required=" + allocatedQuantity);
        }
        
        // Check for time conflicts
        List<ProductionScheduleEntry> conflictingEntries = detectTimeConflicts(shift, estimatedDurationMinutes);
        if (!conflictingEntries.isEmpty()) {
            throw new RuntimeException("Time conflict detected: " + conflictingEntries.size() + 
                " entries would overlap in shift " + shift.getShiftName());
        }
        
        // Validate capacity constraints for resource type
        validateCapacityConstraints(shift, allocatedQuantity);
        
        // Create schedule entry
        ProductionScheduleEntry entry = new ProductionScheduleEntry();
        entry.setProductionShift(shift);
        entry.setProductionOrderLine(orderLine);
        entry.setAllocatedQuantity(allocatedQuantity);
        entry.setEstimatedDurationMinutes(estimatedDurationMinutes);
        entry.setPriority(priority != null ? priority : 5);
        entry.setStatus(ProductionScheduleEntry.ScheduleEntryStatus.SCHEDULED);
        
        ProductionScheduleEntry savedEntry = scheduleEntryRepository.save(entry);
        
        // Allocate capacity in shift
        shift.allocateCapacity(allocatedQuantity);
        shiftRepository.save(shift);
        
        // Update capacity usage for resource type
        updateCapacityUsage(shift, allocatedQuantity, true);
        
        return savedEntry;
    }
    
    /**
     * Reschedule an entry to a different shift.
     */
    public ProductionScheduleEntry rescheduleEntry(Long scheduleEntryId,
                                                   Long newShiftId,
                                                   BigDecimal newAllocatedQuantity) {
        ProductionScheduleEntry entry = scheduleEntryRepository.findById(scheduleEntryId)
            .orElseThrow(() -> new RuntimeException("Schedule entry not found: " + scheduleEntryId));
        
        ProductionShift newShift = shiftRepository.findById(newShiftId)
            .orElseThrow(() -> new RuntimeException("Production shift not found: " + newShiftId));
        
        ProductionShift oldShift = entry.getProductionShift();
        BigDecimal oldQuantity = entry.getAllocatedQuantity();
        
        // Deallocate from old shift
        oldShift.deallocateCapacity(oldQuantity);
        shiftRepository.save(oldShift);
        updateCapacityUsage(oldShift, oldQuantity, false);
        
        // Check new shift capacity
        if (!newShift.canAllocate(newAllocatedQuantity)) {
            throw new RuntimeException("Insufficient capacity in new shift");
        }
        int newDurationMinutes = calculateEstimatedDuration(entry.getProductionOrderLine(), newAllocatedQuantity);
        List<ProductionScheduleEntry> conflicts = detectTimeConflicts(newShift, newDurationMinutes);
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Time conflict in new shift");
        }
        validateCapacityConstraints(newShift, newAllocatedQuantity);
        
        // Allocate to new shift
        entry.setProductionShift(newShift);
        entry.setAllocatedQuantity(newAllocatedQuantity);
        entry.setEstimatedDurationMinutes(newDurationMinutes);
        
        newShift.allocateCapacity(newAllocatedQuantity);
        shiftRepository.save(newShift);
        updateCapacityUsage(newShift, newAllocatedQuantity, true);
        
        return scheduleEntryRepository.save(entry);
    }
    
    /**
     * Confirm a scheduled entry (move to CONFIRMED status).
     */
    public ProductionScheduleEntry confirmEntry(Long scheduleEntryId) {
        ProductionScheduleEntry entry = scheduleEntryRepository.findById(scheduleEntryId)
            .orElseThrow(() -> new RuntimeException("Schedule entry not found: " + scheduleEntryId));
        
        entry.setStatus(ProductionScheduleEntry.ScheduleEntryStatus.CONFIRMED);
        return scheduleEntryRepository.save(entry);
    }
    
    /**
     * Start production for an entry.
     */
    public ProductionScheduleEntry startEntryProduction(Long scheduleEntryId) {
        ProductionScheduleEntry entry = scheduleEntryRepository.findById(scheduleEntryId)
            .orElseThrow(() -> new RuntimeException("Schedule entry not found: " + scheduleEntryId));
        
        entry.setStatus(ProductionScheduleEntry.ScheduleEntryStatus.IN_PROGRESS);
        entry.setActualStartTime(LocalDateTime.now());
        return scheduleEntryRepository.save(entry);
    }
    
    /**
     * Complete production for an entry.
     */
    public ProductionScheduleEntry completeEntryProduction(Long scheduleEntryId) {
        ProductionScheduleEntry entry = scheduleEntryRepository.findById(scheduleEntryId)
            .orElseThrow(() -> new RuntimeException("Schedule entry not found: " + scheduleEntryId));
        
        entry.setStatus(ProductionScheduleEntry.ScheduleEntryStatus.COMPLETED);
        entry.setActualEndTime(LocalDateTime.now());
        return scheduleEntryRepository.save(entry);
    }
    
    /**
     * Cancel a scheduled entry and free up capacity.
     */
    public ProductionScheduleEntry cancelEntry(Long scheduleEntryId, String reason) {
        ProductionScheduleEntry entry = scheduleEntryRepository.findById(scheduleEntryId)
            .orElseThrow(() -> new RuntimeException("Schedule entry not found: " + scheduleEntryId));
        
        if (entry.getStatus() == ProductionScheduleEntry.ScheduleEntryStatus.COMPLETED ||
            entry.getStatus() == ProductionScheduleEntry.ScheduleEntryStatus.IN_PROGRESS) {
            throw new RuntimeException("Cannot cancel an already completed or in-progress entry");
        }
        
        // Free up capacity
        ProductionShift shift = entry.getProductionShift();
        shift.deallocateCapacity(entry.getAllocatedQuantity());
        shiftRepository.save(shift);
        updateCapacityUsage(shift, entry.getAllocatedQuantity(), false);
        
        entry.setStatus(ProductionScheduleEntry.ScheduleEntryStatus.CANCELLED);
        entry.setNotes((reason != null ? reason : "Anulat") + " at " + LocalDateTime.now());
        return scheduleEntryRepository.save(entry);
    }
    
    /**
     * Create or update production capacity for a resource.
     */
    public ProductionCapacity setCapacityLimit(String resourceName,
                                              String resourceType,
                                              LocalDate capacityDate,
                                              BigDecimal maxCapacity,
                                              ProductionCapacity.CapacityUnit unit) {
        Optional<ProductionCapacity> existing = capacityRepository.findByResourceNameAndDate(resourceName, capacityDate);
        
        ProductionCapacity capacity = existing.orElseGet(ProductionCapacity::new);
        capacity.setResourceName(resourceName);
        capacity.setResourceType(resourceType);
        capacity.setCapacityDate(capacityDate);
        capacity.setMaxCapacity(maxCapacity);
        capacity.setCapacityUnit(unit);
        capacity.setIsActive(true);
        
        return capacityRepository.save(capacity);
    }
    
    /**
     * Get capacity status for a specific date and resource type.
     */
    public List<ProductionCapacity> getCapacityStatus(LocalDate date, String resourceType) {
        return capacityRepository.findByDateAndResourceType(date, resourceType);
    }
    
    /**
     * Get scheduling overview for a date range.
     */
    public Map<String, Object> getSchedulingOverview(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        List<ProductionScheduleEntry> entries = scheduleEntryRepository.findScheduledEntriesByDateRange(start, end);
        List<ProductionShift> shifts = shiftRepository.findByDateRange(start, end);
        
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalShifts", shifts.size());
        overview.put("totalScheduledEntries", entries.size());
        overview.put("totalCapacityUtilization", calculateTotalUtilization(shifts));
        overview.put("entriesByStatus", groupEntriesByStatus(entries));
        overview.put("capacityByResourceType", groupCapacityByResourceType(startDate, endDate));
        
        return overview;
    }
    
    /**
     * Detect time conflicts for a given shift and duration.
     */
    private List<ProductionScheduleEntry> detectTimeConflicts(ProductionShift shift, int durationMinutes) {
        // This is a simplified conflict detection
        // In production, would check for scheduling gaps based on shift times
        List<ProductionScheduleEntry> activeEntries = scheduleEntryRepository.findActiveEntriesByShiftId(shift.getId());
        
        // Check total duration doesn't exceed shift time
        long shiftDurationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(shift.getShiftStart(), shift.getShiftEnd());
        long allocatedMinutes = activeEntries.stream()
            .mapToLong(e -> e.getEstimatedDurationMinutes().longValue())
            .sum() + durationMinutes;
        
        if (allocatedMinutes > shiftDurationMinutes) {
            return activeEntries;
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Calculate estimated production duration based on product tech sheet.
     */
    private int calculateEstimatedDuration(ProductionOrderLine orderLine, BigDecimal quantity) {
        Product product = orderLine.getProduct();
        if (product == null) {
            return 60;  // Default 1 hour
        }
        
        Integer prepTime = product.getPrepTimeMinutes() != null ? product.getPrepTimeMinutes() : 0;
        Integer bakingTime = product.getBakingTimeMinutes() != null ? product.getBakingTimeMinutes() : 0;
        
        // Scale by quantity (simplified model)
        BigDecimal baseQuantity = BigDecimal.ONE;
        BigDecimal scaleFactor = quantity.divide(baseQuantity, 2, java.math.RoundingMode.HALF_UP);
        
        return Math.max(30, (int) ((prepTime + bakingTime) * scaleFactor.doubleValue()));
    }
    
    /**
     * Validate capacity constraints for a shift's resource type.
     */
    private void validateCapacityConstraints(ProductionShift shift, BigDecimal allocatedQuantity) {
        LocalDate shiftDate = shift.getShiftStart().toLocalDate();
        List<ProductionCapacity> capacities = capacityRepository.findByDateAndResourceType(
            shiftDate,
            shift.getResourceType().getCode()
        );
        
        for (ProductionCapacity capacity : capacities) {
            if (!capacity.canAllocate(allocatedQuantity)) {
                throw new RuntimeException("Capacity constraint violated for " + capacity.getResourceName() +
                    ". Available: " + capacity.getAvailableCapacity() + ", Required: " + allocatedQuantity);
            }
        }
    }
    
    /**
     * Update capacity usage when allocating/deallocating.
     */
    private void updateCapacityUsage(ProductionShift shift, BigDecimal quantity, boolean isAllocate) {
        LocalDate shiftDate = shift.getShiftStart().toLocalDate();
        List<ProductionCapacity> capacities = capacityRepository.findByDateAndResourceType(
            shiftDate,
            shift.getResourceType().getCode()
        );
        
        for (ProductionCapacity capacity : capacities) {
            if (isAllocate) {
                capacity.addUsage(quantity);
            } else {
                capacity.removeUsage(quantity);
            }
            capacityRepository.save(capacity);
        }
    }
    
    /**
     * Calculate total capacity utilization percentage.
     */
    private double calculateTotalUtilization(List<ProductionShift> shifts) {
        if (shifts.isEmpty()) {
            return 0.0;
        }
        
        BigDecimal totalCapacity = shifts.stream()
            .map(ProductionShift::getCapacityUnits)
            .filter(c -> c != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAllocated = shifts.stream()
            .map(ProductionShift::getAllocatedUnits)
            .filter(c -> c != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        
        return (totalAllocated.doubleValue() / totalCapacity.doubleValue()) * 100.0;
    }
    
    /**
     * Group schedule entries by status.
     */
    private Map<String, Long> groupEntriesByStatus(List<ProductionScheduleEntry> entries) {
        return entries.stream()
            .collect(Collectors.groupingBy(
                e -> e.getStatus().getDisplayName(),
                Collectors.counting()
            ));
    }
    
    /**
     * Group capacity by resource type.
     */
    private Map<String, BigDecimal> groupCapacityByResourceType(LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> capacityMap = new HashMap<>();
        
        for (ProductionShift.ResourceType resourceType : ProductionShift.ResourceType.values()) {
            List<ProductionShift> shifts = shiftRepository.findAvailableShiftsByResourceType(
                resourceType,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
            );
            
            BigDecimal totalCapacity = shifts.stream()
                .map(ProductionShift::getCapacityUnits)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            capacityMap.put(resourceType.getDisplayName(), totalCapacity);
        }
        
        return capacityMap;
    }
}
