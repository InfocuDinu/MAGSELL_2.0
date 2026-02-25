package com.bakerymanager.repository;

import com.bakerymanager.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByMovementDateBetweenOrderByMovementDateDesc(LocalDateTime start, LocalDateTime end);

    List<StockMovement> findByIngredientIdAndMovementTypeInAndMovementDateBetween(
        Long ingredientId,
        List<StockMovement.MovementType> movementTypes,
        LocalDateTime start,
        LocalDateTime end
    );
}
