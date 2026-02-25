package com.bakerymanager.smartbill.reports.financial.adapter;

import com.bakerymanager.entity.*;
import com.bakerymanager.service.FinancialReportingService;
import com.bakerymanager.smartbill.reports.financial.port.FinancialReportingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Adapter that implements the FinancialReportingPort.
 * Delegates to the FinancialReportingService for business logic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialReportingPortAdapter implements FinancialReportingPort {
    
    private final FinancialReportingService financialReportingService;
    
    @Override
    public CostBreakdown calculateProductionCost(Product product, LocalDate costDate) {
        log.debug("Adapter: Calculating production cost for product {}", product.getId());
        return financialReportingService.calculateProductionCost(product, costDate);
    }
    
    @Override
    public DailyFinancialReport generateDailyReport(LocalDate reportDate) {
        log.debug("Adapter: Generating daily report for {}", reportDate);
        return financialReportingService.generateDailyFinancialReport(reportDate);
    }
    
    @Override
    public Map<String, Object> generateMonthlyReport(YearMonth yearMonth) {
        log.debug("Adapter: Generating monthly report for {}", yearMonth);
        return financialReportingService.generateMonthlyFinancialReport(yearMonth);
    }
    
    @Override
    public VATReport generateVATReport(LocalDate startDate, LocalDate endDate, String periodName) {
        log.debug("Adapter: Generating VAT report from {} to {}", startDate, endDate);
        return financialReportingService.generateVATReport(startDate, endDate, periodName);
    }
    
    @Override
    public List<CostBreakdown> getCostReportByDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Adapter: Getting cost report from {} to {}", startDate, endDate);
        return financialReportingService.getCostBreakdownReport(startDate, endDate);
    }
    
    @Override
    public List<DailyFinancialReport> getDailyReportsByDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Adapter: Getting daily reports from {} to {}", startDate, endDate);
        // Will implement in repository
        return null;
    }
    
    @Override
    public List<VATReport> getVATReportsByPeriod(LocalDate startDate, LocalDate endDate) {
        log.debug("Adapter: Getting VAT reports from {} to {}", startDate, endDate);
        // Will implement in repository
        return null;
    }
    
    @Override
    public Map<LocalDate, java.math.BigDecimal> getProfitTrend(YearMonth yearMonth) {
        log.debug("Adapter: Getting profit trend for {}", yearMonth);
        return financialReportingService.getProfitTrend(yearMonth);
    }
    
    @Override
    public List<CostBreakdown> getProductCostHistory(Product product, LocalDate startDate, LocalDate endDate) {
        log.debug("Adapter: Getting cost history for product {} from {} to {}", product.getId(), startDate, endDate);
        // Will implement in repository
        return null;
    }
}
