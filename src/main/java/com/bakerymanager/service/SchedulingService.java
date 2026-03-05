package com.bakerymanager.service;

import com.bakerymanager.entity.*;
import com.bakerymanager.repository.*;
import com.bakerymanager.smartbill.production.api.dto.SchedulerAlertDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionReportRepository productionReportRepository;
    private final ProductService productService;
    
    public SchedulingService(ProductionShiftRepository shiftRepository,
                            ProductionCapacityRepository capacityRepository,
                            ProductionScheduleEntryRepository scheduleEntryRepository,
                            ProductionOrderLineRepository orderLineRepository,
                            ProductionOrderRepository productionOrderRepository,
                            ProductionReportRepository productionReportRepository,
                            ProductService productService) {
        this.shiftRepository = shiftRepository;
        this.capacityRepository = capacityRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.orderLineRepository = orderLineRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.productionReportRepository = productionReportRepository;
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
        if (resourceType == null) {
            return shiftRepository.findAvailableShifts(start, end);
        }
        return shiftRepository.findShiftsWithAvailableCapacity(resourceType, start, end);
    }

    public List<ProductionScheduleEntry> getScheduleEntries(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return scheduleEntryRepository.findEntriesByDateRange(start, end);
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
        overview.put("alertCount", getSchedulerAlerts(startDate, endDate).size());
        
        return overview;
    }

    /**
     * Auto-scheduling engine for unscheduled production order lines.
     */
    public List<ProductionScheduleEntry> autoScheduleOrders(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start.plusDays(7);

        List<ProductionOrder> orders = productionOrderRepository.findByPlannedDateBetweenOrderByPlannedDate(start, end);
        List<ProductionScheduleEntry> createdEntries = new ArrayList<>();

        for (ProductionOrder order : orders) {
            if (order.getStatus() == ProductionOrder.Status.CANCELLED || order.getStatus() == ProductionOrder.Status.COMPLETED) {
                continue;
            }

            ProductionOrder fullOrder = productionOrderRepository.findByIdWithLines(order.getId()).orElse(order);
            for (ProductionOrderLine line : fullOrder.getLines()) {
                if (line == null || line.getId() == null || line.getProduct() == null) {
                    continue;
                }
                if (scheduleEntryRepository.isOrderLineScheduled(line.getId())) {
                    continue;
                }

                BigDecimal pendingQty = pendingQuantity(line);
                if (pendingQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                Optional<ProductionShift> bestShift = findBestShiftForLine(line, pendingQty, start, end);
                if (bestShift.isEmpty()) {
                    continue;
                }

                int priority = computePriority(line, fullOrder);
                ProductionScheduleEntry created = scheduleOrderToShift(line.getId(), bestShift.get().getId(), pendingQty, priority);
                createdEntries.add(created);
            }
        }

        return createdEntries;
    }

    /**
     * Resource leveling - redistribute low-priority work from overloaded shifts to underloaded shifts.
     */
    public int levelResources(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<ProductionShift> shifts = shiftRepository.findByDateRange(start, end);
        List<ProductionShift> overloaded = shifts.stream()
            .filter(s -> utilization(s) >= 90.0)
            .sorted(Comparator.comparingDouble(this::utilization).reversed())
            .toList();

        int moved = 0;
        for (ProductionShift source : overloaded) {
            List<ProductionScheduleEntry> candidates = scheduleEntryRepository.findActiveEntriesByShiftId(source.getId()).stream()
                .sorted(Comparator.comparing(ProductionScheduleEntry::getPriority))
                .toList();

            for (ProductionScheduleEntry entry : candidates) {
                Optional<ProductionShift> targetOpt = shifts.stream()
                    .filter(s -> !Objects.equals(s.getId(), source.getId()))
                    .filter(s -> s.getResourceType() == source.getResourceType())
                    .filter(s -> s.getStatus() == ProductionShift.ShiftStatus.AVAILABLE)
                    .filter(s -> s.canAllocate(entry.getAllocatedQuantity()))
                    .filter(s -> utilization(s) < 80.0)
                    .sorted(Comparator
                        .comparingDouble(this::utilization)
                        .thenComparing(ProductionShift::getShiftStart))
                    .findFirst();

                if (targetOpt.isPresent()) {
                    rescheduleEntry(entry.getId(), targetOpt.get().getId(), entry.getAllocatedQuantity());
                    moved++;
                    if (utilization(source) < 85.0) {
                        break;
                    }
                }
            }
        }

        return moved;
    }

    public List<SchedulerAlertDto> getSchedulerAlerts(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start.plusDays(7);
        LocalDateTime startTs = start.atStartOfDay();
        LocalDateTime endTs = end.atTime(23, 59, 59);

        List<SchedulerAlertDto> alerts = new ArrayList<>();

        // Capacity / over-capacity alerts
        for (ProductionShift shift : shiftRepository.findByDateRange(startTs, endTs)) {
            double u = utilization(shift);
            if (u >= 100.0) {
                alerts.add(new SchedulerAlertDto("CRITICAL", "OVER_CAPACITY",
                    "Schimb supraîncărcat (" + shift.getShiftName() + "): " + String.format(Locale.ROOT, "%.1f%%", u),
                    shift.getResourceId(), shift.getShiftStart()));
            } else if (u >= 90.0) {
                alerts.add(new SchedulerAlertDto("WARN", "HIGH_UTILIZATION",
                    "Schimb aproape de limită (" + shift.getShiftName() + "): " + String.format(Locale.ROOT, "%.1f%%", u),
                    shift.getResourceId(), shift.getShiftStart()));
            }

            if (shift.getStatus() == ProductionShift.ShiftStatus.BLOCKED) {
                alerts.add(new SchedulerAlertDto("WARN", "RESOURCE_BLOCKED",
                    "Resursă indisponibilă: " + shift.getShiftName(), shift.getResourceId(), shift.getShiftStart()));
            }
        }

        // Deadline alerts - unscheduled lines near/over due date
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        for (ProductionOrder order : productionOrderRepository.findByPlannedDateBetweenOrderByPlannedDate(start, end)) {
            if (order.getStatus() == ProductionOrder.Status.CANCELLED || order.getStatus() == ProductionOrder.Status.COMPLETED) {
                continue;
            }
            ProductionOrder fullOrder = productionOrderRepository.findByIdWithLines(order.getId()).orElse(order);
            for (ProductionOrderLine line : fullOrder.getLines()) {
                if (line == null || line.getId() == null || line.getProduct() == null) {
                    continue;
                }
                if (scheduleEntryRepository.isOrderLineScheduled(line.getId())) {
                    continue;
                }
                if (order.getPlannedDate() != null && !order.getPlannedDate().isAfter(tomorrow)) {
                    String severity = order.getPlannedDate().isBefore(LocalDate.now()) ? "CRITICAL" : "WARN";
                    alerts.add(new SchedulerAlertDto(
                        severity,
                        "DEADLINE_RISK",
                        "Linie neplanificată aproape de termen: " + line.getProduct().getName() +
                            " (comandă " + order.getOrderNumber() + ")",
                        order.getOrderNumber(),
                        order.getPlannedDate().atStartOfDay()
                    ));
                }
            }
        }

        return alerts.stream()
            .sorted(Comparator.comparing(SchedulerAlertDto::severity).thenComparing(SchedulerAlertDto::at))
            .toList();
    }

    public Map<String, Object> reconcileExecution(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDateTime startTs = start.atStartOfDay();
        LocalDateTime endTs = end.atTime(23, 59, 59);

        List<ProductionScheduleEntry> entries = scheduleEntryRepository.findEntriesByDateRange(startTs, endTs);
        List<ProductionReport> reports = productionReportRepository.findByProductionDateBetween(startTs, endTs);

        Map<Long, BigDecimal> plannedByProduct = new HashMap<>();
        for (ProductionScheduleEntry e : entries) {
            if (e.getProductionOrderLine() != null && e.getProductionOrderLine().getProduct() != null && e.getAllocatedQuantity() != null) {
                Long productId = e.getProductionOrderLine().getProduct().getId();
                plannedByProduct.merge(productId, e.getAllocatedQuantity(), BigDecimal::add);
            }
        }

        Map<Long, BigDecimal> actualByProduct = new HashMap<>();
        for (ProductionReport r : reports) {
            if (r.getProduct() != null && r.getQuantityProduced() != null) {
                actualByProduct.merge(r.getProduct().getId(), r.getQuantityProduced(), BigDecimal::add);
            }
        }

        List<Map<String, Object>> variance = new ArrayList<>();
        Set<Long> productIds = new HashSet<>();
        productIds.addAll(plannedByProduct.keySet());
        productIds.addAll(actualByProduct.keySet());

        for (Long productId : productIds) {
            Product p = productService.getProductById(productId).orElse(null);
            BigDecimal planned = plannedByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal actual = actualByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal diff = actual.subtract(planned);
            BigDecimal pct = planned.compareTo(BigDecimal.ZERO) > 0
                ? diff.divide(planned, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

            Map<String, Object> row = new HashMap<>();
            row.put("product", p != null ? p.getName() : "P#" + productId);
            row.put("planned", planned);
            row.put("actual", actual);
            row.put("variance", diff);
            row.put("variancePercent", pct);
            variance.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("window", start + " -> " + end);
        result.put("plannedTotal", plannedByProduct.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("actualTotal", actualByProduct.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("varianceByProduct", variance);
        return result;
    }

    public Map<String, Object> getAdvancedSchedulingReport(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start.plusDays(7);
        LocalDateTime startTs = start.atStartOfDay();
        LocalDateTime endTs = end.atTime(23, 59, 59);

        List<ProductionShift> shifts = shiftRepository.findByDateRange(startTs, endTs);
        List<ProductionScheduleEntry> entries = scheduleEntryRepository.findEntriesByDateRange(startTs, endTs);

        List<Map<String, Object>> gantt = entries.stream().map(e -> {
            Map<String, Object> row = new HashMap<>();
            ProductionShift s = e.getProductionShift();
            row.put("shift", s.getShiftName());
            row.put("resource", s.getResourceId());
            row.put("resourceType", s.getResourceType().name());
            row.put("start", s.getShiftStart());
            row.put("end", s.getShiftEnd());
            row.put("status", e.getStatus().name());
            row.put("priority", e.getPriority());
            row.put("product", e.getProductionOrderLine() != null && e.getProductionOrderLine().getProduct() != null
                ? e.getProductionOrderLine().getProduct().getName() : "N/A");
            row.put("qty", e.getAllocatedQuantity());
            return row;
        }).toList();

        Map<String, Double> heatmap = shifts.stream().collect(Collectors.toMap(
            s -> s.getResourceType().name() + "::" + s.getResourceId() + "::" + s.getShiftStart().toLocalDate(),
            this::utilization,
            (a, b) -> Math.max(a, b)
        ));

        List<Map<String, Object>> bottlenecks = shifts.stream()
            .filter(s -> utilization(s) >= 90.0)
            .sorted(Comparator.comparingDouble(this::utilization).reversed())
            .map(s -> {
                Map<String, Object> row = new HashMap<>();
                row.put("shift", s.getShiftName());
                row.put("resource", s.getResourceId());
                row.put("utilization", utilization(s));
                row.put("available", s.getAvailableCapacity());
                return row;
            })
            .toList();

        Map<String, Object> report = new HashMap<>();
        report.put("window", start + " -> " + end);
        report.put("gantt", gantt);
        report.put("heatmap", heatmap);
        report.put("bottlenecks", bottlenecks);
        report.put("alerts", getSchedulerAlerts(start, end));
        report.put("overview", getSchedulingOverview(start, end));
        report.put("execution", reconcileExecution(start, end));
        return report;
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

    private double utilization(ProductionShift shift) {
        if (shift == null || shift.getCapacityUnits() == null || shift.getCapacityUnits().compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        BigDecimal allocated = shift.getAllocatedUnits() != null ? shift.getAllocatedUnits() : BigDecimal.ZERO;
        return allocated.divide(shift.getCapacityUnits(), 6, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .doubleValue();
    }

    private BigDecimal pendingQuantity(ProductionOrderLine line) {
        BigDecimal planned = line.getPlannedQuantity() != null ? line.getPlannedQuantity() : BigDecimal.ZERO;
        BigDecimal actual = line.getActualQuantity() != null ? line.getActualQuantity() : BigDecimal.ZERO;
        BigDecimal pending = planned.subtract(actual);
        if (pending.compareTo(BigDecimal.ZERO) <= 0) {
            return planned;
        }
        return pending;
    }

    private Optional<ProductionShift> findBestShiftForLine(ProductionOrderLine line,
                                                           BigDecimal quantity,
                                                           LocalDate start,
                                                           LocalDate end) {
        ProductionShift.ResourceType preferredType = inferResourceType(line);
        List<ProductionShift> candidates = findAvailableShifts(start.atStartOfDay(), end.atTime(23, 59, 59), preferredType);
        if (candidates.isEmpty()) {
            candidates = findAvailableShifts(start.atStartOfDay(), end.atTime(23, 59, 59), null);
        }

        return candidates.stream()
            .filter(s -> s.canAllocate(quantity))
            .sorted(Comparator
                .comparingDouble(this::utilization)
                .thenComparing(ProductionShift::getShiftStart))
            .findFirst();
    }

    private int computePriority(ProductionOrderLine line, ProductionOrder order) {
        int base = 5;
        if (order != null && order.getPlannedDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), order.getPlannedDate());
            if (days <= 0) {
                base = 10;
            } else if (days == 1) {
                base = 9;
            } else if (days <= 3) {
                base = 7;
            }
        }
        BigDecimal qty = line != null && line.getPlannedQuantity() != null ? line.getPlannedQuantity() : BigDecimal.ZERO;
        if (qty.compareTo(new BigDecimal("500")) > 0) {
            base = Math.min(10, base + 1);
        }
        return base;
    }

    private ProductionShift.ResourceType inferResourceType(ProductionOrderLine line) {
        Product p = line != null ? line.getProduct() : null;
        if (p == null) {
            return ProductionShift.ResourceType.LINE;
        }
        int baking = p.getBakingTimeMinutes() != null ? p.getBakingTimeMinutes() : 0;
        int prep = p.getPrepTimeMinutes() != null ? p.getPrepTimeMinutes() : 0;
        if (baking > prep && baking > 0) {
            return ProductionShift.ResourceType.OVEN;
        }
        if (prep > 0) {
            return ProductionShift.ResourceType.LINE;
        }
        return ProductionShift.ResourceType.LINE;
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
