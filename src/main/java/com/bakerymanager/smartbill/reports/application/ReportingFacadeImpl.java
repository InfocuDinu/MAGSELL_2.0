package com.bakerymanager.smartbill.reports.application;

import com.bakerymanager.entity.*;
import com.bakerymanager.smartbill.reports.api.ReportingFacade;
import com.bakerymanager.smartbill.reports.domain.ReportingPort;
import com.bakerymanager.smartbill.reports.financial.facade.FinancialReportingFacade;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
public class ReportingFacadeImpl implements ReportingFacade {

    private final ReportingPort reportingPort;
    private final FinancialReportingFacade financialReportingFacade;

    public ReportingFacadeImpl(ReportingPort reportingPort, FinancialReportingFacade financialReportingFacade) {
        this.reportingPort = reportingPort;
        this.financialReportingFacade = financialReportingFacade;
    }

    @Override
    public List<Product> getAvailableProducts() {
        return reportingPort.getAvailableProducts();
    }

    @Override
    public List<Ingredient> getAllIngredients() {
        return reportingPort.getAllIngredients();
    }

    @Override
    public List<Ingredient> getLowStockIngredients() {
        return reportingPort.getLowStockIngredients();
    }

    @Override
    public List<IngredientBatch> getExpiringBatches(LocalDate startDate, LocalDate endDate) {
        return reportingPort.getExpiringBatches(startDate, endDate);
    }

    @Override
    public List<StockMovement> getStockMovements(LocalDateTime startDate, LocalDateTime endDate) {
        return reportingPort.getStockMovements(startDate, endDate);
    }

    @Override
    public List<ProductionConsumption> getProductionConsumptions(LocalDate startDate, LocalDate endDate) {
        return reportingPort.getProductionConsumptions(startDate, endDate);
    }

    @Override
    public List<ProductionReport> getProductionReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return reportingPort.getProductionReportsByDateRange(startDate, endDate);
    }

    @Override
    public List<SaleItem> getSaleItems(LocalDateTime startDate, LocalDateTime endDate) {
        return reportingPort.getSaleItems(startDate, endDate);
    }

    @Override
    public List<RecipeItem> getRecipeByProduct(Product product) {
        return reportingPort.getRecipeByProduct(product);
    }

    /**
     * Financial Report Methods - delegate to FinancialReportingFacade
     */
    @Override
    public CostBreakdown calculateProductionCost(Product product, LocalDate costDate) {
        return financialReportingFacade.calculateProductionCost(product, costDate);
    }

    @Override
    public DailyFinancialReport generateDailyReport(LocalDate reportDate) {
        return financialReportingFacade.generateDailyReport(reportDate);
    }

    @Override
    public Map<String, Object> generateMonthlyReport(YearMonth yearMonth) {
        return financialReportingFacade.generateMonthlyReport(yearMonth);
    }

    @Override
    public VATReport generateVATReport(LocalDate startDate, LocalDate endDate, String periodName) {
        return financialReportingFacade.generateVATReport(startDate, endDate, periodName);
    }

    @Override
    public List<CostBreakdown> getCostReportByDateRange(LocalDate startDate, LocalDate endDate) {
        return financialReportingFacade.getCostReportByDateRange(startDate, endDate);
    }

    @Override
    public List<DailyFinancialReport> getDailyReportsByDateRange(LocalDate startDate, LocalDate endDate) {
        return financialReportingFacade.getDailyReportsByDateRange(startDate, endDate);
    }

    @Override
    public List<VATReport> getVATReportsByPeriod(LocalDate startDate, LocalDate endDate) {
        return financialReportingFacade.getVATReportsByPeriod(startDate, endDate);
    }

    @Override
    public Map<LocalDate, java.math.BigDecimal> getProfitTrend(YearMonth yearMonth) {
        return financialReportingFacade.getProfitTrend(yearMonth);
    }

    @Override
    public List<CostBreakdown> getProductCostHistory(Product product, LocalDate startDate, LocalDate endDate) {
        return financialReportingFacade.getProductCostHistory(product, startDate, endDate);
    }
}
