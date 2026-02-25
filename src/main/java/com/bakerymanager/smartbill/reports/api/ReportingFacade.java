package com.bakerymanager.smartbill.reports.api;

import com.bakerymanager.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public interface ReportingFacade {

    List<Product> getAvailableProducts();

    List<Ingredient> getAllIngredients();

    List<Ingredient> getLowStockIngredients();

    List<IngredientBatch> getExpiringBatches(LocalDate startDate, LocalDate endDate);

    List<StockMovement> getStockMovements(LocalDateTime startDate, LocalDateTime endDate);

    List<ProductionConsumption> getProductionConsumptions(LocalDate startDate, LocalDate endDate);

    List<ProductionReport> getProductionReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<SaleItem> getSaleItems(LocalDateTime startDate, LocalDateTime endDate);

    List<RecipeItem> getRecipeByProduct(Product product);

    /**
     * Financial Report Methods (Point 9)
     */
    CostBreakdown calculateProductionCost(Product product, LocalDate costDate);

    DailyFinancialReport generateDailyReport(LocalDate reportDate);

    Map<String, Object> generateMonthlyReport(YearMonth yearMonth);

    VATReport generateVATReport(LocalDate startDate, LocalDate endDate, String periodName);

    List<CostBreakdown> getCostReportByDateRange(LocalDate startDate, LocalDate endDate);

    List<DailyFinancialReport> getDailyReportsByDateRange(LocalDate startDate, LocalDate endDate);

    List<VATReport> getVATReportsByPeriod(LocalDate startDate, LocalDate endDate);

    Map<LocalDate, java.math.BigDecimal> getProfitTrend(YearMonth yearMonth);

    List<CostBreakdown> getProductCostHistory(Product product, LocalDate startDate, LocalDate endDate);
}
