package com.bakerymanager.smartbill.reports.infrastructure;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.IngredientBatch;
import com.bakerymanager.entity.StockMovement;
import com.bakerymanager.entity.ProductionConsumption;
import com.bakerymanager.entity.SaleItem;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.repository.IngredientBatchRepository;
import com.bakerymanager.repository.StockMovementRepository;
import com.bakerymanager.repository.ProductionConsumptionRepository;
import com.bakerymanager.repository.SaleItemRepository;
import com.bakerymanager.repository.RecipeItemRepository;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.ProductService;
import com.bakerymanager.smartbill.reports.domain.ReportingPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReportingPortAdapter implements ReportingPort {

    private final ProductService productService;
    private final IngredientService ingredientService;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductionConsumptionRepository productionConsumptionRepository;
    private final SaleItemRepository saleItemRepository;
    private final RecipeItemRepository recipeItemRepository;

    public ReportingPortAdapter(ProductService productService,
                                IngredientService ingredientService,
                                IngredientBatchRepository ingredientBatchRepository,
                                StockMovementRepository stockMovementRepository,
                                ProductionConsumptionRepository productionConsumptionRepository,
                                SaleItemRepository saleItemRepository,
                                RecipeItemRepository recipeItemRepository) {
        this.productService = productService;
        this.ingredientService = ingredientService;
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productionConsumptionRepository = productionConsumptionRepository;
        this.saleItemRepository = saleItemRepository;
        this.recipeItemRepository = recipeItemRepository;
    }

    @Override
    public List<Product> getAvailableProducts() {
        return productService.getAvailableProducts();
    }

    @Override
    public List<Ingredient> getAllIngredients() {
        return ingredientService.getAllIngredients();
    }

    @Override
    public List<Ingredient> getLowStockIngredients() {
        return ingredientService.getLowStockIngredients();
    }

    @Override
    public List<IngredientBatch> getExpiringBatches(LocalDate startDate, LocalDate endDate) {
        return ingredientBatchRepository.findByExpiryDateBetween(startDate, endDate);
    }

    @Override
    public List<StockMovement> getStockMovements(LocalDateTime startDate, LocalDateTime endDate) {
        return stockMovementRepository.findByMovementDateBetweenOrderByMovementDateDesc(startDate, endDate);
    }

    @Override
    public List<ProductionConsumption> getProductionConsumptions(LocalDate startDate, LocalDate endDate) {
        return productionConsumptionRepository.findByPlannedDateBetween(startDate, endDate);
    }

    @Override
    public List<SaleItem> getSaleItems(LocalDateTime startDate, LocalDateTime endDate) {
        return saleItemRepository.findBySaleDateBetween(startDate, endDate);
    }

    @Override
    public List<RecipeItem> getRecipeByProduct(Product product) {
        return recipeItemRepository.findByProductWithIngredient(product);
    }
}
