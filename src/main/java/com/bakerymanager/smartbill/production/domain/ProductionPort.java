package com.bakerymanager.smartbill.production.domain;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.ProductionOrder;
import com.lowagie.text.DocumentException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductionPort {

    List<Product> getActiveProducts();

    Product saveProduct(Product product);

    Optional<Ingredient> getIngredientById(Long ingredientId);

    List<Ingredient> getAllIngredients();

    List<RecipeItem> getRecipeByProduct(Product product);

    RecipeItem addRecipeItem(Long productId, Long ingredientId, BigDecimal requiredQuantity);

    RecipeItem addRecipeProductItem(Long productId, Long sourceProductId, BigDecimal requiredQuantity);

    void removeRecipeItem(Long recipeItemId);

    boolean canProduce(Long productId, BigDecimal quantity);

    void executeProduction(Long productId, BigDecimal quantity);

    Map<Ingredient, BigDecimal> calculateRequiredIngredients(Long productId, BigDecimal quantity);

    List<ProductionReport> getAllProductionReports();

    ProductionReport saveProductionReport(ProductionReport report);

    List<ProductionReport> getProductionReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    void exportProductionReportPdf(ProductionReport report, String filePath) throws IOException, DocumentException;

    ProductionOrder createProductionOrder(LocalDate plannedDate, String notes);

    ProductionOrder addProductionOrderLine(Long productionOrderId, Long productId, BigDecimal plannedQuantity);

    List<ProductionOrder> getProductionOrders(LocalDate startDate, LocalDate endDate);

    ProductionOrder getProductionOrderWithLines(Long productionOrderId);

    ProductionOrder startProductionOrder(Long productionOrderId);

    ProductionOrder completeProductionOrder(Long productionOrderId);
}
