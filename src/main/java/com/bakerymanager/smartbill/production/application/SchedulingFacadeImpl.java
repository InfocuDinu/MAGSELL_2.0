package com.bakerymanager.smartbill.production.application;

import com.bakerymanager.entity.*;
import com.bakerymanager.smartbill.production.api.SchedulingFacade;
import com.bakerymanager.smartbill.production.api.dto.SchedulerAlertDto;
import com.bakerymanager.smartbill.production.domain.SchedulingPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Implementation of SchedulingFacade.
 */
@Service
public class SchedulingFacadeImpl implements SchedulingFacade {
    
    private final SchedulingPort schedulingPort;
    
    public SchedulingFacadeImpl(SchedulingPort schedulingPort) {
        this.schedulingPort = schedulingPort;
    }
    
    @Override
    public ProductionShift createShift(String shiftName,
                                      LocalDateTime shiftStart,
                                      LocalDateTime shiftEnd,
                                      ProductionShift.ResourceType resourceType,
                                      String resourceId,
                                      BigDecimal capacityUnits) {
        return schedulingPort.createShift(shiftName, shiftStart, shiftEnd, resourceType, resourceId, capacityUnits);
    }
    
    @Override
    public List<ProductionShift> findAvailableShifts(LocalDateTime start,
                                                    LocalDateTime end,
                                                    ProductionShift.ResourceType resourceType) {
        return schedulingPort.findAvailableShifts(start, end, resourceType);
    }
    
    @Override
    public ProductionShift getShiftById(Long shiftId) {
        return schedulingPort.getShiftById(shiftId);
    }
    
    @Override
    public void deleteShift(Long shiftId) {
        schedulingPort.deleteShift(shiftId);
    }
    
    @Override
    public ProductionScheduleEntry scheduleOrderToShift(Long orderLineId,
                                                        Long shiftId,
                                                        BigDecimal allocatedQuantity,
                                                        Integer priority) {
        return schedulingPort.scheduleOrderToShift(orderLineId, shiftId, allocatedQuantity, priority);
    }
    
    @Override
    public ProductionScheduleEntry rescheduleEntry(Long scheduleEntryId,
                                                   Long newShiftId,
                                                   BigDecimal newAllocatedQuantity) {
        return schedulingPort.rescheduleEntry(scheduleEntryId, newShiftId, newAllocatedQuantity);
    }
    
    @Override
    public ProductionScheduleEntry confirmEntry(Long scheduleEntryId) {
        return schedulingPort.confirmEntry(scheduleEntryId);
    }
    
    @Override
    public ProductionScheduleEntry startEntryProduction(Long scheduleEntryId) {
        return schedulingPort.startEntryProduction(scheduleEntryId);
    }
    
    @Override
    public ProductionScheduleEntry completeEntryProduction(Long scheduleEntryId) {
        return schedulingPort.completeEntryProduction(scheduleEntryId);
    }
    
    @Override
    public ProductionScheduleEntry cancelEntry(Long scheduleEntryId, String reason) {
        return schedulingPort.cancelEntry(scheduleEntryId, reason);
    }
    
    @Override
    public ProductionScheduleEntry getScheduleEntryById(Long scheduleEntryId) {
        return schedulingPort.getScheduleEntryById(scheduleEntryId);
    }
    
    @Override
    public List<ProductionScheduleEntry> getScheduleEntriesByShift(Long shiftId) {
        return schedulingPort.getScheduleEntriesByShift(shiftId);
    }
    
    @Override
    public ProductionCapacity setCapacityLimit(String resourceName,
                                              String resourceType,
                                              LocalDate capacityDate,
                                              BigDecimal maxCapacity,
                                              ProductionCapacity.CapacityUnit unit) {
        return schedulingPort.setCapacityLimit(resourceName, resourceType, capacityDate, maxCapacity, unit);
    }
    
    @Override
    public List<ProductionCapacity> getCapacityStatus(LocalDate date, String resourceType) {
        return schedulingPort.getCapacityStatus(date, resourceType);
    }
    
    @Override
    public ProductionCapacity getCapacityById(Long capacityId) {
        return schedulingPort.getCapacityById(capacityId);
    }
    
    @Override
    public Map<String, Object> getSchedulingOverview(LocalDate startDate, LocalDate endDate) {
        return schedulingPort.getSchedulingOverview(startDate, endDate);
    }

    @Override
    public List<ProductionScheduleEntry> getScheduleEntries(LocalDate startDate, LocalDate endDate) {
        return schedulingPort.getScheduleEntries(startDate, endDate);
    }

    @Override
    public List<ProductionScheduleEntry> autoScheduleOrders(LocalDate startDate, LocalDate endDate) {
        return schedulingPort.autoScheduleOrders(startDate, endDate);
    }

    @Override
    public int levelResources(LocalDate startDate, LocalDate endDate) {
        return schedulingPort.levelResources(startDate, endDate);
    }

    @Override
    public List<SchedulerAlertDto> getSchedulerAlerts(LocalDate startDate, LocalDate endDate) {
        return schedulingPort.getSchedulerAlerts(startDate, endDate);
    }

    @Override
    public Map<String, Object> reconcileExecution(LocalDate startDate, LocalDate endDate) {
        return schedulingPort.reconcileExecution(startDate, endDate);
    }

    @Override
    public Map<String, Object> getAdvancedSchedulingReport(LocalDate startDate, LocalDate endDate) {
        return schedulingPort.getAdvancedSchedulingReport(startDate, endDate);
    }
}
