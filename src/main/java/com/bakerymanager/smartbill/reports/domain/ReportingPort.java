package com.bakerymanager.smartbill.reports.domain;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.IngredientBatch;
import com.bakerymanager.entity.StockMovement;
import com.bakerymanager.entity.ProductionConsumption;
import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.SaleItem;
import com.bakerymanager.entity.RecipeItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportingPort {

    List<Product> getAvailableProducts();

    List<Ingredient> getAllIngredients();

    List<Ingredient> getLowStockIngredients();

    List<IngredientBatch> getExpiringBatches(LocalDate startDate, LocalDate endDate);

    List<StockMovement> getStockMovements(LocalDateTime startDate, LocalDateTime endDate);

    List<ProductionConsumption> getProductionConsumptions(LocalDate startDate, LocalDate endDate);

    List<ProductionReport> getProductionReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<SaleItem> getSaleItems(LocalDateTime startDate, LocalDateTime endDate);

    List<RecipeItem> getRecipeByProduct(Product product);
}
