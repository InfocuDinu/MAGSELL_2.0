package com.bakerymanager.service;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.IngredientBatch;
import com.bakerymanager.entity.StockMovement;
import com.bakerymanager.repository.IngredientBatchRepository;
import com.bakerymanager.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    private final IngredientBatchRepository ingredientBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UnitConversionService unitConversionService;

    public StockService(IngredientBatchRepository ingredientBatchRepository,
                        StockMovementRepository stockMovementRepository,
                        UnitConversionService unitConversionService) {
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.unitConversionService = unitConversionService;
    }

    @Transactional
    public IngredientBatch receiveBatch(Ingredient ingredient,
                                        BigDecimal quantity,
                                        String unit,
                                        LocalDate expiryDate,
                                        LocalDateTime receivedDate,
                                        String batchCode,
                                        String sourceType,
                                        Long sourceId) {
        String targetUnit = resolveUnit(unit, ingredient);
        BigDecimal normalizedQty = unitConversionService.convert(quantity, unit, targetUnit);

        IngredientBatch batch = new IngredientBatch();
        batch.setIngredient(ingredient);
        batch.setQuantity(normalizedQty);
        batch.setExpiryDate(expiryDate);
        batch.setReceivedDate(receivedDate != null ? receivedDate : LocalDateTime.now());
        batch.setBatchCode(batchCode);
        batch.setSourceType(sourceType);
        batch.setSourceId(sourceId);

        IngredientBatch saved = ingredientBatchRepository.save(batch);

        StockMovement movement = new StockMovement();
        movement.setIngredient(ingredient);
        movement.setBatch(saved);
        movement.setMovementType(StockMovement.MovementType.RECEIPT);
        movement.setQuantity(normalizedQty);
        movement.setUnit(targetUnit);
        movement.setMovementDate(LocalDateTime.now());
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        stockMovementRepository.save(movement);

        return saved;
    }

    @Transactional
    public List<StockMovement> consumeFefo(Ingredient ingredient,
                                          BigDecimal quantityNeeded,
                                          String unit,
                                          String sourceType,
                                          Long sourceId) {
        String targetUnit = resolveUnit(unit, ingredient);
        BigDecimal normalizedNeeded = unitConversionService.convert(quantityNeeded, unit, targetUnit);
        List<IngredientBatch> batches = ingredientBatchRepository
                .findAvailableBatchesForIngredient(ingredient.getId());

        BigDecimal remaining = normalizedNeeded;
        List<StockMovement> movements = new ArrayList<>();

        for (IngredientBatch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal available = batch.getQuantity();
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal used = available.min(remaining);
            batch.setQuantity(available.subtract(used));
            ingredientBatchRepository.save(batch);

            StockMovement movement = new StockMovement();
            movement.setIngredient(ingredient);
            movement.setBatch(batch);
            movement.setMovementType(StockMovement.MovementType.CONSUMPTION);
            movement.setQuantity(used);
            movement.setUnit(targetUnit);
            movement.setMovementDate(LocalDateTime.now());
            movement.setSourceType(sourceType);
            movement.setSourceId(sourceId);
            movements.add(stockMovementRepository.save(movement));

            remaining = remaining.subtract(used);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Insufficient batch stock for ingredient " +
                (ingredient != null ? ingredient.getName() : "unknown") +
                ". Missing: " + remaining);
        }

        return movements;
    }

    private String resolveUnit(String unit, Ingredient ingredient) {
        if (unit != null && !unit.isBlank()) {
            return unit;
        }
        return ingredient != null && ingredient.getUnitOfMeasure() != null
                ? ingredient.getUnitOfMeasure().name()
                : "";
    }
}
