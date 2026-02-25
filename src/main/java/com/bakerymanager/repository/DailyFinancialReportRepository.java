package com.bakerymanager.repository;

import com.bakerymanager.entity.DailyFinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyFinancialReportRepository extends JpaRepository<DailyFinancialReport, Long> {
    
    Optional<DailyFinancialReport> findByReportDate(LocalDate reportDate);
    
    @Query("""
        SELECT dfr FROM DailyFinancialReport dfr 
        WHERE dfr.reportDate BETWEEN :startDate AND :endDate
        ORDER BY dfr.reportDate DESC
        """)
    List<DailyFinancialReport> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("""
        SELECT dfr FROM DailyFinancialReport dfr 
        WHERE YEAR(dfr.reportDate) = :year AND MONTH(dfr.reportDate) = :month
        ORDER BY dfr.reportDate DESC
        """)
    List<DailyFinancialReport> findByYearAndMonth(
        @Param("year") Integer year,
        @Param("month") Integer month
    );
    
    @Query("""
        SELECT dfr FROM DailyFinancialReport dfr 
        WHERE YEAR(dfr.reportDate) = :year
        ORDER BY dfr.reportDate DESC
        """)
    List<DailyFinancialReport> findByYear(@Param("year") Integer year);
}
