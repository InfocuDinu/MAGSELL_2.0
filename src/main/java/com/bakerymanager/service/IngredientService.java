package com.bakerymanager.service;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.repository.IngredientRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class IngredientService {
    
    private final IngredientRepository ingredientRepository;
    
    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }
    
    @Cacheable("ingredientsAll")
    public List<Ingredient> getAllIngredients() {
        return ingredientRepository.findAll();
    }
    
    @Cacheable(value = "ingredientsById", key = "#id", unless = "#id == null")
    public Optional<Ingredient> getIngredientById(Long id) {
        return ingredientRepository.findById(id);
    }
    
    public Optional<Ingredient> getIngredientByName(String name) {
        return ingredientRepository.findByName(name);
    }
    
    public List<Ingredient> findByName(String name) {
        Optional<Ingredient> ingredient = ingredientRepository.findByName(name);
        return ingredient.map(List::of).orElse(List.of());
    }
    
    public List<Ingredient> findByNameContainingIgnoreCase(String name) {
        return ingredientRepository.findByNameContainingIgnoreCase(name);
    }

    @Cacheable(value = "ingredientsByBarcode", key = "#barcode", condition = "#barcode != null && !#barcode.trim().isEmpty()")
    public List<Ingredient> findByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return List.of();
        }
        return ingredientRepository.findByBarcodeNormalized(barcode.trim());
    }
    
    @Caching(evict = {
        @CacheEvict(value = "ingredientsAll", allEntries = true),
        @CacheEvict(value = "ingredientsAvailable", allEntries = true),
        @CacheEvict(value = "ingredientsLowStock", allEntries = true),
        @CacheEvict(value = "ingredientsById", allEntries = true),
        @CacheEvict(value = "ingredientsByBarcode", allEntries = true)
    })
    public Ingredient saveIngredient(Ingredient ingredient) {
        return ingredientRepository.save(ingredient);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "ingredientsAll", allEntries = true),
        @CacheEvict(value = "ingredientsAvailable", allEntries = true),
        @CacheEvict(value = "ingredientsLowStock", allEntries = true),
        @CacheEvict(value = "ingredientsById", allEntries = true),
        @CacheEvict(value = "ingredientsByBarcode", allEntries = true)
    })
    public Ingredient createIngredient(String name, Ingredient.UnitOfMeasure unitOfMeasure, 
                                     BigDecimal currentStock, BigDecimal lastPurchasePrice) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setUnitOfMeasure(unitOfMeasure);
        ingredient.setCurrentStock(currentStock != null ? currentStock : BigDecimal.ZERO);
        ingredient.setLastPurchasePrice(lastPurchasePrice);
        ingredient.setMinimumStock(BigDecimal.ZERO);
        return ingredientRepository.save(ingredient);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "ingredientsAll", allEntries = true),
        @CacheEvict(value = "ingredientsAvailable", allEntries = true),
        @CacheEvict(value = "ingredientsLowStock", allEntries = true),
        @CacheEvict(value = "ingredientsById", allEntries = true),
        @CacheEvict(value = "ingredientsByBarcode", allEntries = true)
    })
    public void deleteIngredient(Long id) {
        ingredientRepository.deleteById(id);
    }
    
    public List<Ingredient> searchIngredients(String searchTerm) {
        return ingredientRepository.findByNameContainingIgnoreCase(searchTerm);
    }
    
    @Cacheable("ingredientsLowStock")
    public List<Ingredient> getLowStockIngredients() {
        return ingredientRepository.findLowStockIngredients();
    }
    
    @Cacheable("ingredientsAvailable")
    public List<Ingredient> getAvailableIngredients() {
        return ingredientRepository.findAvailableIngredients();
    }
    
    @Caching(evict = {
        @CacheEvict(value = "ingredientsAll", allEntries = true),
        @CacheEvict(value = "ingredientsAvailable", allEntries = true),
        @CacheEvict(value = "ingredientsLowStock", allEntries = true),
        @CacheEvict(value = "ingredientsById", allEntries = true),
        @CacheEvict(value = "ingredientsByBarcode", allEntries = true)
    })
    public void addStock(Long ingredientId, BigDecimal quantity) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
            .orElseThrow(() -> new RuntimeException("Ingredient not found: " + ingredientId));
        ingredient.setCurrentStock(ingredient.getCurrentStock().add(quantity));
        ingredientRepository.save(ingredient);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "ingredientsAll", allEntries = true),
        @CacheEvict(value = "ingredientsAvailable", allEntries = true),
        @CacheEvict(value = "ingredientsLowStock", allEntries = true),
        @CacheEvict(value = "ingredientsById", allEntries = true),
        @CacheEvict(value = "ingredientsByBarcode", allEntries = true)
    })
    public void removeStock(Long ingredientId, BigDecimal quantity) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
            .orElseThrow(() -> new RuntimeException("Ingredient not found: " + ingredientId));
        
        if (ingredient.getCurrentStock().compareTo(quantity) < 0) {
            throw new RuntimeException("Insufficient stock for ingredient: " + ingredient.getName());
        }
        
        ingredient.setCurrentStock(ingredient.getCurrentStock().subtract(quantity));
        ingredientRepository.save(ingredient);
    }
    
    public boolean hasSufficientStock(Long ingredientId, BigDecimal requiredQuantity) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
            .orElseThrow(() -> new RuntimeException("Ingredient not found: " + ingredientId));
        return ingredient.getCurrentStock().compareTo(requiredQuantity) >= 0;
    }
    
    @Caching(evict = {
        @CacheEvict(value = "ingredientsAll", allEntries = true),
        @CacheEvict(value = "ingredientsAvailable", allEntries = true),
        @CacheEvict(value = "ingredientsLowStock", allEntries = true),
        @CacheEvict(value = "ingredientsById", allEntries = true),
        @CacheEvict(value = "ingredientsByBarcode", allEntries = true)
    })
    public void updatePurchasePrice(Long ingredientId, BigDecimal newPrice) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
            .orElseThrow(() -> new RuntimeException("Ingredient not found: " + ingredientId));
        ingredient.setLastPurchasePrice(newPrice);
        ingredientRepository.save(ingredient);
    }
    
    public long countAvailableIngredients() {
        return ingredientRepository.countAvailableIngredients();
    }
    
    public long countLowStockIngredients() {
        return ingredientRepository.countLowStockIngredients();
    }
}
