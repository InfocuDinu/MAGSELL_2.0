package com.bakerymanager.smartbill.reports.application;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.IngredientBatch;
import com.bakerymanager.entity.StockMovement;
import com.bakerymanager.entity.ProductionConsumption;
import com.bakerymanager.entity.SaleItem;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.smartbill.reports.api.ReportingFacade;
import com.bakerymanager.smartbill.reports.domain.ReportingPort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportingFacadeImpl implements ReportingFacade {

    private final ReportingPort reportingPort;

    public ReportingFacadeImpl(ReportingPort reportingPort) {
        this.reportingPort = reportingPort;
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
    public List<SaleItem> getSaleItems(LocalDateTime startDate, LocalDateTime endDate) {
        return reportingPort.getSaleItems(startDate, endDate);
    }

    @Override
    public List<RecipeItem> getRecipeByProduct(Product product) {
        return reportingPort.getRecipeByProduct(product);
    }
}
