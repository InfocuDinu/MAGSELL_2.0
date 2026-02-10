package com.bakerymanager.service;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.repository.RecipeItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductionService {
    
    private final RecipeItemRepository recipeItemRepository;
    private final ProductService productService;
    private final IngredientService ingredientService;
    
    public ProductionService(RecipeItemRepository recipeItemRepository, 
                           ProductService productService, 
                           IngredientService ingredientService) {
        this.recipeItemRepository = recipeItemRepository;
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
        
        // Combined verification and removal in a single loop
        for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
            Long ingredientId = entry.getKey();
            BigDecimal requiredQty = entry.getValue();
            
            if (!ingredientService.hasSufficientStock(ingredientId, requiredQty)) {
                Ingredient ingredient = ingredientService.getIngredientById(ingredientId).get();
                throw new RuntimeException("Insufficient stock for ingredient: " + ingredient.getName() + 
                    ". Required: " + requiredQty + ", Available: " + ingredient.getCurrentStock());
            }
            
            ingredientService.removeStock(ingredientId, requiredQty);
        }
        
        productService.addStock(productId, quantity);
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
