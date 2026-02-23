package com.bakerymanager.smartbill.production.application;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.smartbill.production.api.ProductionFacade;
import com.bakerymanager.smartbill.production.domain.ProductionPort;
import com.lowagie.text.DocumentException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductionFacadeImpl implements ProductionFacade {

    private final ProductionPort productionPort;

    public ProductionFacadeImpl(ProductionPort productionPort) {
        this.productionPort = productionPort;
    }

    @Override
    public List<Product> getActiveProducts() {
        return productionPort.getActiveProducts();
    }

    @Override
    public Product saveProduct(Product product) {
        return productionPort.saveProduct(product);
    }

    @Override
    public Optional<Ingredient> getIngredientById(Long ingredientId) {
        return productionPort.getIngredientById(ingredientId);
    }

    @Override
    public List<Ingredient> getAllIngredients() {
        return productionPort.getAllIngredients();
    }

    @Override
    public List<RecipeItem> getRecipeByProduct(Product product) {
        return productionPort.getRecipeByProduct(product);
    }

    @Override
    public RecipeItem addRecipeItem(Long productId, Long ingredientId, BigDecimal requiredQuantity) {
        return productionPort.addRecipeItem(productId, ingredientId, requiredQuantity);
    }

    @Override
    public void removeRecipeItem(Long recipeItemId) {
        productionPort.removeRecipeItem(recipeItemId);
    }

    @Override
    public boolean canProduce(Long productId, BigDecimal quantity) {
        return productionPort.canProduce(productId, quantity);
    }

    @Override
    public void executeProduction(Long productId, BigDecimal quantity) {
        productionPort.executeProduction(productId, quantity);
    }

    @Override
    public Map<Ingredient, BigDecimal> calculateRequiredIngredients(Long productId, BigDecimal quantity) {
        return productionPort.calculateRequiredIngredients(productId, quantity);
    }

    @Override
    public List<ProductionReport> getAllProductionReports() {
        return productionPort.getAllProductionReports();
    }

    @Override
    public ProductionReport saveProductionReport(ProductionReport report) {
        return productionPort.saveProductionReport(report);
    }

    @Override
    public List<ProductionReport> getProductionReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return productionPort.getProductionReportsByDateRange(startDate, endDate);
    }

    @Override
    public void exportProductionReportPdf(ProductionReport report, String filePath) throws IOException, DocumentException {
        productionPort.exportProductionReportPdf(report, filePath);
    }
}
