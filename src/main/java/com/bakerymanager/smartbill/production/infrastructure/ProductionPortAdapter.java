package com.bakerymanager.smartbill.production.infrastructure;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.ProductionOrder;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.PdfService;
import com.bakerymanager.service.ProductService;
import com.bakerymanager.service.ProductionService;
import com.bakerymanager.smartbill.production.domain.ProductionPort;
import com.lowagie.text.DocumentException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ProductionPortAdapter implements ProductionPort {

    private final ProductionService productionService;
    private final ProductService productService;
    private final IngredientService ingredientService;
    private final PdfService pdfService;

    public ProductionPortAdapter(ProductionService productionService,
                                 ProductService productService,
                                 IngredientService ingredientService,
                                 PdfService pdfService) {
        this.productionService = productionService;
        this.productService = productService;
        this.ingredientService = ingredientService;
        this.pdfService = pdfService;
    }

    @Override
    public List<Product> getActiveProducts() {
        return productService.getActiveProducts();
    }

    @Override
    public Product saveProduct(Product product) {
        return productService.saveProduct(product);
    }

    @Override
    public Optional<Ingredient> getIngredientById(Long ingredientId) {
        return ingredientService.getIngredientById(ingredientId);
    }

    @Override
    public List<Ingredient> getAllIngredients() {
        return ingredientService.getAllIngredients();
    }

    @Override
    public List<RecipeItem> getRecipeByProduct(Product product) {
        return productionService.getRecipeByProduct(product);
    }

    @Override
    public RecipeItem addRecipeItem(Long productId, Long ingredientId, BigDecimal requiredQuantity) {
        return productionService.addRecipeItem(productId, ingredientId, requiredQuantity);
    }

    @Override
    public RecipeItem addRecipeProductItem(Long productId, Long sourceProductId, BigDecimal requiredQuantity) {
        return productionService.addRecipeProductItem(productId, sourceProductId, requiredQuantity);
    }

    @Override
    public void removeRecipeItem(Long recipeItemId) {
        productionService.removeRecipeItem(recipeItemId);
    }

    @Override
    public boolean canProduce(Long productId, BigDecimal quantity) {
        return productionService.canProduce(productId, quantity);
    }

    @Override
    public void executeProduction(Long productId, BigDecimal quantity) {
        productionService.executeProduction(productId, quantity);
    }

    @Override
    public Map<Ingredient, BigDecimal> calculateRequiredIngredients(Long productId, BigDecimal quantity) {
        return productionService.calculateRequiredIngredients(productId, quantity);
    }

    @Override
    public List<ProductionReport> getAllProductionReports() {
        return productionService.getAllProductionReports();
    }

    @Override
    public ProductionReport saveProductionReport(ProductionReport report) {
        return productionService.saveProductionReport(report);
    }

    @Override
    public List<ProductionReport> getProductionReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return productionService.getProductionReportsByDateRange(startDate, endDate);
    }

    @Override
    public void exportProductionReportPdf(ProductionReport report, String filePath) throws IOException, DocumentException {
        pdfService.generateProductionReportPdf(report, filePath);
    }

    @Override
    public ProductionOrder createProductionOrder(LocalDate plannedDate, String notes) {
        return productionService.createProductionOrder(plannedDate, notes);
    }

    @Override
    public ProductionOrder addProductionOrderLine(Long productionOrderId, Long productId, BigDecimal plannedQuantity) {
        return productionService.addProductionOrderLine(productionOrderId, productId, plannedQuantity);
    }

    @Override
    public List<ProductionOrder> getProductionOrders(LocalDate startDate, LocalDate endDate) {
        return productionService.getProductionOrders(startDate, endDate);
    }

    @Override
    public ProductionOrder getProductionOrderWithLines(Long productionOrderId) {
        return productionService.getProductionOrderWithLines(productionOrderId);
    }

    @Override
    public ProductionOrder startProductionOrder(Long productionOrderId) {
        return productionService.startProductionOrder(productionOrderId);
    }

    @Override
    public ProductionOrder completeProductionOrder(Long productionOrderId) {
        return productionService.completeProductionOrder(productionOrderId);
    }
}
