package com.bakerymanager.repository;

import com.bakerymanager.entity.ProductionShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionShiftRepository extends JpaRepository<ProductionShift, Long> {
    
    @Query("SELECT s FROM ProductionShift s WHERE s.shiftStart >= :start AND s.shiftEnd <= :end ORDER BY s.shiftStart ASC")
    List<ProductionShift> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    
    @Query("SELECT s FROM ProductionShift s WHERE s.resourceType = :resourceType AND s.status = 'AVAILABLE' " +
           "AND s.shiftStart >= :start AND s.shiftEnd <= :end ORDER BY s.shiftStart ASC")
    List<ProductionShift> findAvailableShiftsByResourceType(
            @Param("resourceType") ProductionShift.ResourceType resourceType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    
    @Query("SELECT s FROM ProductionShift s WHERE s.capacityDate = :date ORDER BY s.shiftStart ASC")
    List<ProductionShift> findByDate(@Param("date") LocalDate date);
    
    @Query("SELECT s FROM ProductionShift s WHERE s.shiftStart >= :start AND s.shiftEnd <= :end " +
           "AND s.status != 'BLOCKED' ORDER BY s.shiftStart ASC")
    List<ProductionShift> findAvailableShifts(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    
    @Query("SELECT s FROM ProductionShift s WHERE s.resourceId = :resourceId ORDER BY s.shiftStart DESC")
    List<ProductionShift> findByResourceId(@Param("resourceId") String resourceId);
    
    @Query("SELECT s FROM ProductionShift s WHERE s.resourceType = :resourceType " +
           "AND s.shiftStart >= :start AND s.shiftEnd <= :end AND s.status = 'AVAILABLE' " +
           "AND s.allocatedUnits < s.capacityUnits ORDER BY s.shiftStart ASC")
    List<ProductionShift> findShiftsWithAvailableCapacity(
            @Param("resourceType") ProductionShift.ResourceType resourceType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
