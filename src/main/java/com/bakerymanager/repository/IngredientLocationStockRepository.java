package com.bakerymanager.repository;

import com.bakerymanager.entity.IngredientLocationStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientLocationStockRepository extends JpaRepository<IngredientLocationStock, Long> {

    Optional<IngredientLocationStock> findByIngredientIdAndLocationId(Long ingredientId, Long locationId);

    List<IngredientLocationStock> findByIngredientId(Long ingredientId);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM IngredientLocationStock s WHERE s.ingredient.id = :ingredientId")
    BigDecimal sumQuantityByIngredientId(@Param("ingredientId") Long ingredientId);

    @Query("SELECT s FROM IngredientLocationStock s JOIN FETCH s.ingredient i JOIN FETCH s.location l " +
           "ORDER BY i.name ASC, l.warehouseName ASC, l.zoneName ASC")
    List<IngredientLocationStock> findAllWithIngredientAndLocation();

    @Query("SELECT s FROM IngredientLocationStock s JOIN FETCH s.location l " +
           "WHERE s.ingredient.id = :ingredientId " +
           "ORDER BY l.warehouseName ASC, l.zoneName ASC")
    List<IngredientLocationStock> findByIngredientIdWithLocation(@Param("ingredientId") Long ingredientId);
}
