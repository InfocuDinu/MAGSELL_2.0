package com.bakerymanager.repository;

import com.bakerymanager.entity.ProductionCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionCapacityRepository extends JpaRepository<ProductionCapacity, Long> {
    
    @Query("SELECT c FROM ProductionCapacity c WHERE c.capacityDate = :date AND c.isActive = true ORDER BY c.resourceType ASC, c.resourceName ASC")
    List<ProductionCapacity> findByDateAndActive(@Param("date") LocalDate date);
    
    @Query("SELECT c FROM ProductionCapacity c WHERE c.capacityDate = :date AND c.resourceType = :resourceType " +
           "AND c.isActive = true ORDER BY c.resourceName ASC")
    List<ProductionCapacity> findByDateAndResourceType(
            @Param("date") LocalDate date,
            @Param("resourceType") String resourceType
    );
    
    @Query("SELECT c FROM ProductionCapacity c WHERE c.resourceType = :resourceType AND c.isActive = true " +
           "AND c.capacityDate BETWEEN :startDate AND :endDate ORDER BY c.capacityDate ASC")
    List<ProductionCapacity> findByResourceTypeAndDateRange(
            @Param("resourceType") String resourceType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT c FROM ProductionCapacity c WHERE c.resourceName = :resourceName AND c.capacityDate = :date")
    Optional<ProductionCapacity> findByResourceNameAndDate(
            @Param("resourceName") String resourceName,
            @Param("date") LocalDate date
    );
    
    @Query("SELECT c FROM ProductionCapacity c WHERE c.isActive = true AND c.capacityDate = :date " +
           "AND c.currentUsage < c.maxCapacity ORDER BY c.resourceType ASC")
    List<ProductionCapacity> findAvailableCapacities(@Param("date") LocalDate date);
}
