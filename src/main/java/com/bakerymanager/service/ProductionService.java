package com.bakerymanager.service;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.repository.RecipeItemRepository;
import com.bakerymanager.repository.ProductionReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductionService {
    
    private final RecipeItemRepository recipeItemRepository;
    private final ProductionReportRepository productionReportRepository;
    private final ProductService productService;
    private final IngredientService ingredientService;
    
    public ProductionService(RecipeItemRepository recipeItemRepository,
                           ProductionReportRepository productionReportRepository,
                           ProductService productService, 
                           IngredientService ingredientService) {
        this.recipeItemRepository = recipeItemRepository;
        this.productionReportRepository = productionReportRepository;
        this.productService = productService;
        this.ingredientService = ingredientService;
    }
    
    public List<RecipeItem> getRecipeByProduct(Long productId) {
        return recipeItemRepository.findByProductId(productId);
    }
    
    public List<RecipeItem> getRecipeByProduct(Product product) {
        return recipeItemRepository.findByProduct(product);
    }
    
    public RecipeItem addRecipeItem(Long productId, Long ingredientId, BigDecimal requiredQuantity) {
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        Ingredient ingredient = ingredientService.getIngredientById(ingredientId)
            .orElseThrow(() -> new RuntimeException("Ingredient not found: " + ingredientId));
        
        Optional<RecipeItem> existingItem = recipeItemRepository.findByProductAndIngredient(product, ingredient);
        if (existingItem.isPresent()) {
            RecipeItem item = existingItem.get();
            item.setRequiredQuantity(requiredQuantity);
            return recipeItemRepository.save(item);
        } else {
            RecipeItem newItem = new RecipeItem();
            newItem.setProduct(product);
            newItem.setIngredient(ingredient);
            newItem.setRequiredQuantity(requiredQuantity);
            return recipeItemRepository.save(newItem);
        }
    }
    
    public void removeRecipeItem(Long recipeItemId) {
        recipeItemRepository.deleteById(recipeItemId);
    }
    
    public void removeRecipeItem(Long productId, Long ingredientId) {
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        Ingredient ingredient = ingredientService.getIngredientById(ingredientId)
            .orElseThrow(() -> new RuntimeException("Ingredient not found: " + ingredientId));
        
        recipeItemRepository.findByProductAndIngredient(product, ingredient)
            .ifPresent(item -> recipeItemRepository.delete(item));
    }
    
    public void executeProduction(Long productId, BigDecimal quantity) {
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        List<RecipeItem> recipeItems = recipeItemRepository.findByProduct(product);
        if (recipeItems.isEmpty()) {
            throw new RuntimeException("No recipe defined for product: " + product.getName());
        }
        
        Map<Long, BigDecimal> requiredIngredients = recipeItems.stream()
            .collect(Collectors.toMap(
                item -> item.getIngredient().getId(),
                item -> item.getTotalRequiredQuantity(quantity)
            ));
        
        // Verify all ingredients have sufficient stock first (atomic check)
        for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
            if (!ingredientService.hasSufficientStock(entry.getKey(), entry.getValue())) {
                Ingredient ingredient = ingredientService.getIngredientById(entry.getKey()).get();
                throw new RuntimeException("Insufficient stock for ingredient: " + ingredient.getName() + 
                    ". Required: " + entry.getValue() + ", Available: " + ingredient.getCurrentStock());
            }
        }
        
        // All checks passed, now remove stock from all ingredients
        for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
            ingredientService.removeStock(entry.getKey(), entry.getValue());
        }
        
        // Add product stock
        productService.addStock(productId, quantity);
        
        // Create production report
        ProductionReport report = new ProductionReport();
        report.setProduct(product);
        report.setQuantityProduced(quantity);
        report.setProductionDate(LocalDateTime.now());
        report.setStatus(ProductionReport.ProductionStatus.COMPLETED);
        productionReportRepository.save(report);
    }
    
    public List<ProductionReport> getAllProductionReports() {
        return productionReportRepository.findAllOrderByProductionDateDesc();
    }
    
    public List<ProductionReport> getProductionReportsByProduct(Product product) {
        return productionReportRepository.findByProductOrderByProductionDateDesc(product);
    }
    
    public List<ProductionReport> getProductionReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return productionReportRepository.findByProductionDateBetween(startDate, endDate);
    }
    
    public Map<Ingredient, BigDecimal> calculateRequiredIngredients(Long productId, BigDecimal quantity) {
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        List<RecipeItem> recipeItems = recipeItemRepository.findByProduct(product);
        
        return recipeItems.stream()
            .collect(Collectors.toMap(
                RecipeItem::getIngredient,
                item -> item.getTotalRequiredQuantity(quantity)
            ));
    }
    
    public boolean canProduce(Long productId, BigDecimal quantity) {
        try {
            Map<Ingredient, BigDecimal> requiredIngredients = calculateRequiredIngredients(productId, quantity);
            for (Map.Entry<Ingredient, BigDecimal> entry : requiredIngredients.entrySet()) {
                if (!ingredientService.hasSufficientStock(entry.getKey().getId(), entry.getValue())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
