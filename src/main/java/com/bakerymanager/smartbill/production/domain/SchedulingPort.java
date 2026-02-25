package com.bakerymanager.smartbill.production.domain;

import com.bakerymanager.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Port for production scheduling operations.
 */
public interface SchedulingPort {
    
    // Shift Management
    ProductionShift createShift(String shiftName,
                               LocalDateTime shiftStart,
                               LocalDateTime shiftEnd,
                               ProductionShift.ResourceType resourceType,
                               String resourceId,
                               BigDecimal capacityUnits);
    
    List<ProductionShift> findAvailableShifts(LocalDateTime start,
                                             LocalDateTime end,
                                             ProductionShift.ResourceType resourceType);
    
    ProductionShift getShiftById(Long shiftId);
    
    void deleteShift(Long shiftId);
    
    // Schedule Entry Management
    ProductionScheduleEntry scheduleOrderToShift(Long orderLineId,
                                                Long shiftId,
                                                BigDecimal allocatedQuantity,
                                                Integer priority);
    
    ProductionScheduleEntry rescheduleEntry(Long scheduleEntryId,
                                           Long newShiftId,
                                           BigDecimal newAllocatedQuantity);
    
    ProductionScheduleEntry confirmEntry(Long scheduleEntryId);
    
    ProductionScheduleEntry startEntryProduction(Long scheduleEntryId);
    
    ProductionScheduleEntry completeEntryProduction(Long scheduleEntryId);
    
    ProductionScheduleEntry cancelEntry(Long scheduleEntryId, String reason);
    
    ProductionScheduleEntry getScheduleEntryById(Long scheduleEntryId);
    
    List<ProductionScheduleEntry> getScheduleEntriesByShift(Long shiftId);
    
    // Capacity Management
    ProductionCapacity setCapacityLimit(String resourceName,
                                       String resourceType,
                                       LocalDate capacityDate,
                                       BigDecimal maxCapacity,
                                       ProductionCapacity.CapacityUnit unit);
    
    List<ProductionCapacity> getCapacityStatus(LocalDate date, String resourceType);
    
    ProductionCapacity getCapacityById(Long capacityId);
    
    // Reporting & Analysis
    Map<String, Object> getSchedulingOverview(LocalDate startDate, LocalDate endDate);
}
