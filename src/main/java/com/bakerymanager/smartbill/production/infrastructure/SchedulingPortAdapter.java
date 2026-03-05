package com.bakerymanager.smartbill.production.infrastructure;

import com.bakerymanager.entity.*;
import com.bakerymanager.smartbill.production.api.dto.SchedulerAlertDto;
import com.bakerymanager.repository.ProductionCapacityRepository;
import com.bakerymanager.repository.ProductionScheduleEntryRepository;
import com.bakerymanager.repository.ProductionShiftRepository;
import com.bakerymanager.service.SchedulingService;
import com.bakerymanager.smartbill.production.domain.SchedulingPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Adapter implementation for SchedulingPort using SchedulingService.
 */
@Component
public class SchedulingPortAdapter implements SchedulingPort {
    
    private final SchedulingService schedulingService;
    private final ProductionShiftRepository shiftRepository;
    private final ProductionCapacityRepository capacityRepository;
    private final ProductionScheduleEntryRepository scheduleEntryRepository;
    
    public SchedulingPortAdapter(SchedulingService schedulingService,
                                ProductionShiftRepository shiftRepository,
                                ProductionCapacityRepository capacityRepository,
                                ProductionScheduleEntryRepository scheduleEntryRepository) {
        this.schedulingService = schedulingService;
        this.shiftRepository = shiftRepository;
        this.capacityRepository = capacityRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
    }
    
    @Override
    public ProductionShift createShift(String shiftName,
                                      LocalDateTime shiftStart,
                                      LocalDateTime shiftEnd,
                                      ProductionShift.ResourceType resourceType,
                                      String resourceId,
                                      BigDecimal capacityUnits) {
        return schedulingService.createShift(shiftName, shiftStart, shiftEnd, resourceType, resourceId, capacityUnits);
    }
    
    @Override
    public List<ProductionShift> findAvailableShifts(LocalDateTime start,
                                                    LocalDateTime end,
                                                    ProductionShift.ResourceType resourceType) {
        return schedulingService.findAvailableShifts(start, end, resourceType);
    }
    
    @Override
    public ProductionShift getShiftById(Long shiftId) {
        return shiftRepository.findById(shiftId)
            .orElseThrow(() -> new RuntimeException("Production shift not found: " + shiftId));
    }
    
    @Override
    public void deleteShift(Long shiftId) {
        shiftRepository.deleteById(shiftId);
    }
    
    @Override
    public ProductionScheduleEntry scheduleOrderToShift(Long orderLineId,
                                                        Long shiftId,
                                                        BigDecimal allocatedQuantity,
                                                        Integer priority) {
        return schedulingService.scheduleOrderToShift(orderLineId, shiftId, allocatedQuantity, priority);
    }
    
    @Override
    public ProductionScheduleEntry rescheduleEntry(Long scheduleEntryId,
                                                   Long newShiftId,
                                                   BigDecimal newAllocatedQuantity) {
        return schedulingService.rescheduleEntry(scheduleEntryId, newShiftId, newAllocatedQuantity);
    }
    
    @Override
    public ProductionScheduleEntry confirmEntry(Long scheduleEntryId) {
        return schedulingService.confirmEntry(scheduleEntryId);
    }
    
    @Override
    public ProductionScheduleEntry startEntryProduction(Long scheduleEntryId) {
        return schedulingService.startEntryProduction(scheduleEntryId);
    }
    
    @Override
    public ProductionScheduleEntry completeEntryProduction(Long scheduleEntryId) {
        return schedulingService.completeEntryProduction(scheduleEntryId);
    }
    
    @Override
    public ProductionScheduleEntry cancelEntry(Long scheduleEntryId, String reason) {
        return schedulingService.cancelEntry(scheduleEntryId, reason);
    }
    
    @Override
    public ProductionScheduleEntry getScheduleEntryById(Long scheduleEntryId) {
        return scheduleEntryRepository.findById(scheduleEntryId)
            .orElseThrow(() -> new RuntimeException("Schedule entry not found: " + scheduleEntryId));
    }
    
    @Override
    public List<ProductionScheduleEntry> getScheduleEntriesByShift(Long shiftId) {
        return scheduleEntryRepository.findByShiftId(shiftId);
    }
    
    @Override
    public ProductionCapacity setCapacityLimit(String resourceName,
                                              String resourceType,
                                              LocalDate capacityDate,
                                              BigDecimal maxCapacity,
                                              ProductionCapacity.CapacityUnit unit) {
        return schedulingService.setCapacityLimit(resourceName, resourceType, capacityDate, maxCapacity, unit);
    }
    
    @Override
    public List<ProductionCapacity> getCapacityStatus(LocalDate date, String resourceType) {
        return schedulingService.getCapacityStatus(date, resourceType);
    }
    
    @Override
    public ProductionCapacity getCapacityById(Long capacityId) {
        return capacityRepository.findById(capacityId)
            .orElseThrow(() -> new RuntimeException("Production capacity not found: " + capacityId));
    }
    
    @Override
    public Map<String, Object> getSchedulingOverview(LocalDate startDate, LocalDate endDate) {
        return schedulingService.getSchedulingOverview(startDate, endDate);
    }

    @Override
    public List<ProductionScheduleEntry> getScheduleEntries(LocalDate startDate, LocalDate endDate) {
        return schedulingService.getScheduleEntries(startDate, endDate);
    }

    @Override
    public List<ProductionScheduleEntry> autoScheduleOrders(LocalDate startDate, LocalDate endDate) {
        return schedulingService.autoScheduleOrders(startDate, endDate);
    }

    @Override
    public int levelResources(LocalDate startDate, LocalDate endDate) {
        return schedulingService.levelResources(startDate, endDate);
    }

    @Override
    public List<SchedulerAlertDto> getSchedulerAlerts(LocalDate startDate, LocalDate endDate) {
        return schedulingService.getSchedulerAlerts(startDate, endDate);
    }

    @Override
    public Map<String, Object> reconcileExecution(LocalDate startDate, LocalDate endDate) {
        return schedulingService.reconcileExecution(startDate, endDate);
    }

    @Override
    public Map<String, Object> getAdvancedSchedulingReport(LocalDate startDate, LocalDate endDate) {
        return schedulingService.getAdvancedSchedulingReport(startDate, endDate);
    }
}
