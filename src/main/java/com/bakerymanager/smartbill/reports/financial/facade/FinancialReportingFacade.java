package com.bakerymanager.smartbill.reports.financial.facade;

import com.bakerymanager.entity.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Facade interface for financial reporting operations.
 * Extends the basic ReportingFacade with financial-specific methods.
 */
public interface FinancialReportingFacade {
    
    /**
     * Calculate production cost for a product on a specific date.
     */
    CostBreakdown calculateProductionCost(Product product, LocalDate costDate);
    
    /**
     * Generate daily financial summary.
     */
    DailyFinancialReport generateDailyReport(LocalDate reportDate);
    
    /**
     * Generate monthly financial summary.
     */
    Map<String, Object> generateMonthlyReport(YearMonth yearMonth);
    
    /**
     * Generate VAT compliance report.
     */
    VATReport generateVATReport(LocalDate startDate, LocalDate endDate, String periodName);
    
    /**
     * Get cost breakdown report for date range.
     */
    List<CostBreakdown> getCostReportByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get daily reports for date range.
     */
    List<DailyFinancialReport> getDailyReportsByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get VAT reports.
     */
    List<VATReport> getVATReportsByPeriod(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get profit trend for visualization.
     */
    Map<LocalDate, java.math.BigDecimal> getProfitTrend(YearMonth yearMonth);
    
    /**
     * Get product cost history.
     */
    List<CostBreakdown> getProductCostHistory(Product product, LocalDate startDate, LocalDate endDate);
}
