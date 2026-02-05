package com.bakerymanager.repository;

import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeItemRepository extends JpaRepository<RecipeItem, Long> {
    
    List<RecipeItem> findByProduct(Product product);
    
    List<RecipeItem> findByIngredient(Ingredient ingredient);
    
    Optional<RecipeItem> findByProductAndIngredient(Product product, Ingredient ingredient);
    
    @Query("SELECT ri FROM RecipeItem ri WHERE ri.product.id = :productId")
    List<RecipeItem> findByProductId(@Param("productId") Long productId);
    
    @Query("SELECT ri FROM RecipeItem ri WHERE ri.ingredient.id = :ingredientId")
    List<RecipeItem> findByIngredientId(@Param("ingredientId") Long ingredientId);
    
    @Query("SELECT ri FROM RecipeItem ri JOIN ri.product p WHERE p.isActive = true")
    List<RecipeItem> findByActiveProducts();
    
    @Query("SELECT ri FROM RecipeItem ri WHERE ri.product = :product ORDER BY ri.ingredient.name")
    List<RecipeItem> findByProductOrderByIngredientName(@Param("product") Product product);
}
