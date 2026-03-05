package com.bakerymanager.repository;

import com.bakerymanager.entity.ProductionScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionScheduleEntryRepository extends JpaRepository<ProductionScheduleEntry, Long> {
    
    @Query("SELECT e FROM ProductionScheduleEntry e WHERE e.productionShift.id = :shiftId " +
           "ORDER BY e.priority DESC, e.createdAt ASC")
    List<ProductionScheduleEntry> findByShiftId(@Param("shiftId") Long shiftId);
    
    @Query("SELECT e FROM ProductionScheduleEntry e WHERE e.productionOrderLine.id = :orderLineId")
    Optional<ProductionScheduleEntry> findByOrderLineId(@Param("orderLineId") Long orderLineId);
    
    @Query("SELECT e FROM ProductionScheduleEntry e WHERE e.status = 'SCHEDULED' " +
           "AND e.productionShift.shiftStart >= :start AND e.productionShift.shiftEnd <= :end " +
           "ORDER BY e.priority DESC, e.productionShift.shiftStart ASC")
    List<ProductionScheduleEntry> findScheduledEntriesByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT e FROM ProductionScheduleEntry e " +
          "JOIN FETCH e.productionShift s " +
          "JOIN FETCH e.productionOrderLine ol " +
          "JOIN FETCH ol.product p " +
          "WHERE s.shiftStart >= :start AND s.shiftEnd <= :end " +
          "ORDER BY s.shiftStart ASC, e.priority DESC")
    List<ProductionScheduleEntry> findEntriesByDateRange(
           @Param("start") LocalDateTime start,
           @Param("end") LocalDateTime end
    );
    
    @Query("SELECT e FROM ProductionScheduleEntry e WHERE e.productionShift.id = :shiftId " +
           "AND e.status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS') ORDER BY e.priority DESC")
    List<ProductionScheduleEntry> findActiveEntriesByShiftId(@Param("shiftId") Long shiftId);
    
    @Query("SELECT COUNT(e) > 0 FROM ProductionScheduleEntry e WHERE e.productionOrderLine.id = :orderLineId " +
           "AND e.status != 'CANCELLED'")
    boolean isOrderLineScheduled(@Param("orderLineId") Long orderLineId);
    
    @Query("SELECT e FROM ProductionScheduleEntry e WHERE e.productionShift.resourceType = :resourceType " +
           "AND e.productionShift.shiftStart >= :start AND e.productionShift.shiftEnd <= :end " +
           "AND e.status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS') ORDER BY e.productionShift.shiftStart ASC")
    List<ProductionScheduleEntry> findByResourceTypeAndDateRange(
            @Param("resourceType") String resourceType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
