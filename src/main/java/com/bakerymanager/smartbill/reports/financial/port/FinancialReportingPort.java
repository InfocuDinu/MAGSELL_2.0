package com.bakerymanager.smartbill.reports.financial.port;

import com.bakerymanager.entity.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Port interface for financial reporting operations.
 * Defines the contract for financial report generation and queries.
 */
public interface FinancialReportingPort {
    
    /**
     * Calculate and persist production cost breakdown for a product on a specific date.
     */
    CostBreakdown calculateProductionCost(Product product, LocalDate costDate);
    
    /**
     * Generate daily financial report for a specific date.
     */
    DailyFinancialReport generateDailyReport(LocalDate reportDate);
    
    /**
     * Generate monthly financial aggregation.
     */
    Map<String, Object> generateMonthlyReport(YearMonth yearMonth);
    
    /**
     * Generate VAT compliance report for a date range.
     */
    VATReport generateVATReport(LocalDate startDate, LocalDate endDate, String periodName);
    
    /**
     * Get cost breakdown details for a date range.
     */
    List<CostBreakdown> getCostReportByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get daily financial reports for a date range.
     */
    List<DailyFinancialReport> getDailyReportsByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get VAT reports for a period overlap.
     */
    List<VATReport> getVATReportsByPeriod(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get profit trend data for a month.
     */
    Map<LocalDate, java.math.BigDecimal> getProfitTrend(YearMonth yearMonth);
    
    /**
     * Get cost breakdown for a specific product and date range.
     */
    List<CostBreakdown> getProductCostHistory(Product product, LocalDate startDate, LocalDate endDate);
}
