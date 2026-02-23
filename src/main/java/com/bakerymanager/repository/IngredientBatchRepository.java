package com.bakerymanager.repository;

import com.bakerymanager.entity.IngredientBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IngredientBatchRepository extends JpaRepository<IngredientBatch, Long> {

    @Query("SELECT b FROM IngredientBatch b " +
           "WHERE b.ingredient.id = :ingredientId AND b.quantity > 0 " +
           "ORDER BY CASE WHEN b.expiryDate IS NULL THEN 1 ELSE 0 END, b.expiryDate ASC, b.receivedDate ASC")
    List<IngredientBatch> findAvailableBatchesForIngredient(@Param("ingredientId") Long ingredientId);

    List<IngredientBatch> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);
}
