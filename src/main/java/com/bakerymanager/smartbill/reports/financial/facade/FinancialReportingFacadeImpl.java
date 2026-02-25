package com.bakerymanager.smartbill.reports.financial.facade;

import com.bakerymanager.entity.*;
import com.bakerymanager.smartbill.reports.financial.port.FinancialReportingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Facade implementation for financial reporting.
 * Delegates to the port/adapter layer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialReportingFacadeImpl implements FinancialReportingFacade {
    
    private final FinancialReportingPort financialReportingPort;
    
    @Override
    public CostBreakdown calculateProductionCost(Product product, LocalDate costDate) {
        log.info("Facade: Calculate production cost for product {}", product.getId());
        return financialReportingPort.calculateProductionCost(product, costDate);
    }
    
    @Override
    public DailyFinancialReport generateDailyReport(LocalDate reportDate) {
        log.info("Facade: Generate daily report for {}", reportDate);
        return financialReportingPort.generateDailyReport(reportDate);
    }
    
    @Override
    public Map<String, Object> generateMonthlyReport(YearMonth yearMonth) {
        log.info("Facade: Generate monthly report for {}", yearMonth);
        return financialReportingPort.generateMonthlyReport(yearMonth);
    }
    
    @Override
    public VATReport generateVATReport(LocalDate startDate, LocalDate endDate, String periodName) {
        log.info("Facade: Generate VAT report for {} to {}", startDate, endDate);
        return financialReportingPort.generateVATReport(startDate, endDate, periodName);
    }
    
    @Override
    public List<CostBreakdown> getCostReportByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Facade: Get cost report from {} to {}", startDate, endDate);
        return financialReportingPort.getCostReportByDateRange(startDate, endDate);
    }
    
    @Override
    public List<DailyFinancialReport> getDailyReportsByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Facade: Get daily reports from {} to {}", startDate, endDate);
        return financialReportingPort.getDailyReportsByDateRange(startDate, endDate);
    }
    
    @Override
    public List<VATReport> getVATReportsByPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("Facade: Get VAT reports from {} to {}", startDate, endDate);
        return financialReportingPort.getVATReportsByPeriod(startDate, endDate);
    }
    
    @Override
    public Map<LocalDate, java.math.BigDecimal> getProfitTrend(YearMonth yearMonth) {
        log.info("Facade: Get profit trend for {}", yearMonth);
        return financialReportingPort.getProfitTrend(yearMonth);
    }
    
    @Override
    public List<CostBreakdown> getProductCostHistory(Product product, LocalDate startDate, LocalDate endDate) {
        log.info("Facade: Get product cost history for {} from {} to {}", product.getId(), startDate, endDate);
        return financialReportingPort.getProductCostHistory(product, startDate, endDate);
    }
}
