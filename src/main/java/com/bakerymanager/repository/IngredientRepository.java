package com.bakerymanager.repository;

import com.bakerymanager.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    
    Optional<Ingredient> findByName(String name);
    
    List<Ingredient> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT i FROM Ingredient i WHERE i.currentStock <= i.minimumStock")
    List<Ingredient> findLowStockIngredients();
    
    @Query("SELECT i FROM Ingredient i WHERE i.currentStock > 0 ORDER BY i.name")
    List<Ingredient> findAvailableIngredients();
    
    @Query("SELECT i FROM Ingredient i WHERE i.unitOfMeasure = :unitOfMeasure")
    List<Ingredient> findByUnitOfMeasure(@Param("unitOfMeasure") Ingredient.UnitOfMeasure unitOfMeasure);
    
    @Query("SELECT COUNT(i) FROM Ingredient i WHERE i.currentStock > 0")
    long countAvailableIngredients();
    
    @Query("SELECT COUNT(i) FROM Ingredient i WHERE i.currentStock <= i.minimumStock")
    long countLowStockIngredients();
}
