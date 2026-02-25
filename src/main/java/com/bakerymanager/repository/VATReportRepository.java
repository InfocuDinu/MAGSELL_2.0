package com.bakerymanager.repository;

import com.bakerymanager.entity.VATReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VATReportRepository extends JpaRepository<VATReport, Long> {
    
    @Query("""
        SELECT vr FROM VATReport vr 
        WHERE vr.reportStartDate <= :endDate AND vr.reportEndDate >= :startDate
        ORDER BY vr.reportEndDate DESC
        """)
    List<VATReport> findByPeriodOverlap(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("""
        SELECT vr FROM VATReport vr 
        WHERE YEAR(vr.reportStartDate) = :year AND MONTH(vr.reportStartDate) = :month
        ORDER BY vr.reportEndDate DESC
        """)
    List<VATReport> findByYearAndMonth(
        @Param("year") Integer year,
        @Param("month") Integer month
    );
    
    @Query("""
        SELECT vr FROM VATReport vr 
        WHERE YEAR(vr.reportStartDate) = :year
        ORDER BY vr.reportEndDate DESC
        """)
    List<VATReport> findByYear(@Param("year") Integer year);
}
