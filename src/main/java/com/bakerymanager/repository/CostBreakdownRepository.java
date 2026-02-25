package com.bakerymanager.repository;

import com.bakerymanager.entity.CostBreakdown;
import com.bakerymanager.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CostBreakdownRepository extends JpaRepository<CostBreakdown, Long> {
    
    @Query("""
        SELECT cb FROM CostBreakdown cb 
        WHERE cb.product = :product AND cb.costDate = :costDate
        """)
    Optional<CostBreakdown> findByProductAndDate(
        @Param("product") Product product,
        @Param("costDate") LocalDate costDate
    );
    
    @Query("""
        SELECT cb FROM CostBreakdown cb 
        WHERE cb.product = :product AND cb.costDate BETWEEN :startDate AND :endDate
        ORDER BY cb.costDate DESC
        """)
    List<CostBreakdown> findByProductAndDateRange(
        @Param("product") Product product,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("""
        SELECT cb FROM CostBreakdown cb 
        WHERE cb.costDate BETWEEN :startDate AND :endDate
        ORDER BY cb.costDate DESC, cb.product.name ASC
        """)
    List<CostBreakdown> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("""
        SELECT cb FROM CostBreakdown cb 
        WHERE cb.costDate = :costDate
        ORDER BY cb.product.name ASC
        """)
    List<CostBreakdown> findByDate(@Param("costDate") LocalDate costDate);
    
    @Query("""
        SELECT cb FROM CostBreakdown cb 
        WHERE YEAR(cb.costDate) = :year AND MONTH(cb.costDate) = :month
        ORDER BY cb.costDate DESC, cb.product.name ASC
        """)
    List<CostBreakdown> findByYearAndMonth(
        @Param("year") Integer year,
        @Param("month") Integer month
    );
}
