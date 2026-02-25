package com.bakerymanager.service;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.StockMovement;
import com.bakerymanager.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReplenishmentService {

    private final IngredientService ingredientService;
    private final StockMovementRepository stockMovementRepository;

    public ReplenishmentService(IngredientService ingredientService,
                                StockMovementRepository stockMovementRepository) {
        this.ingredientService = ingredientService;
        this.stockMovementRepository = stockMovementRepository;
    }

    public List<ReplenishmentSuggestion> generateTodaySuggestions() {
        return generateSuggestions(30, 3);
    }

    public List<ReplenishmentSuggestion> generateSuggestions(int lookbackDays, int leadTimeDays) {
        int safeLookback = Math.max(1, lookbackDays);
        int safeLeadTime = Math.max(1, leadTimeDays);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(safeLookback).withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<StockMovement.MovementType> outflowTypes = List.of(
            StockMovement.MovementType.CONSUMPTION,
            StockMovement.MovementType.WASTE,
            StockMovement.MovementType.RETURN
        );

        return ingredientService.getAllIngredients().stream()
            .map(ingredient -> buildSuggestion(ingredient, outflowTypes, start, now, safeLookback, safeLeadTime))
            .filter(ReplenishmentSuggestion::shouldOrderToday)
            .sorted(Comparator
                .comparing(ReplenishmentSuggestion::priorityOrder)
                .thenComparing(ReplenishmentSuggestion::recommendedOrderQty, Comparator.reverseOrder())
                .thenComparing(ReplenishmentSuggestion::ingredientName))
            .collect(Collectors.toList());
    }

    private ReplenishmentSuggestion buildSuggestion(Ingredient ingredient,
                                                    List<StockMovement.MovementType> outflowTypes,
                                                    LocalDateTime start,
                                                    LocalDateTime end,
                                                    int lookbackDays,
                                                    int leadTimeDays) {
        BigDecimal currentStock = nz(ingredient.getCurrentStock());
        BigDecimal safetyStock = nz(ingredient.getMinimumStock());

        List<StockMovement> outflows = stockMovementRepository.findByIngredientIdAndMovementTypeInAndMovementDateBetween(
            ingredient.getId(),
            outflowTypes,
            start,
            end
        );

        BigDecimal totalOutflow = outflows.stream()
            .map(StockMovement::getQuantity)
            .filter(q -> q != null && q.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgDailyConsumption = totalOutflow
            .divide(BigDecimal.valueOf(lookbackDays), 6, RoundingMode.HALF_UP);

        BigDecimal reorderPoint = safetyStock
            .add(avgDailyConsumption.multiply(BigDecimal.valueOf(leadTimeDays)))
            .setScale(3, RoundingMode.HALF_UP);

        BigDecimal recommended = reorderPoint.subtract(currentStock);
        if (recommended.compareTo(BigDecimal.ZERO) < 0) {
            recommended = BigDecimal.ZERO;
        }
        recommended = recommended.setScale(3, RoundingMode.HALF_UP);

        return new ReplenishmentSuggestion(
            ingredient.getId(),
            ingredient.getName(),
            ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : "",
            ingredient.getWarehouse(),
            ingredient.getZone(),
            currentStock.setScale(3, RoundingMode.HALF_UP),
            safetyStock.setScale(3, RoundingMode.HALF_UP),
            avgDailyConsumption.setScale(3, RoundingMode.HALF_UP),
            reorderPoint,
            recommended,
            derivePriority(currentStock, reorderPoint, avgDailyConsumption)
        );
    }

    private String derivePriority(BigDecimal current, BigDecimal reorderPoint, BigDecimal avgDaily) {
        if (current.compareTo(BigDecimal.ZERO) <= 0) {
            return "CRITIC";
        }
        if (current.compareTo(reorderPoint.multiply(BigDecimal.valueOf(0.5))) <= 0) {
            return "RIDICAT";
        }
        if (avgDaily.compareTo(BigDecimal.ZERO) > 0 && current.compareTo(reorderPoint) < 0) {
            return "MEDIU";
        }
        return "NORMAL";
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public record ReplenishmentSuggestion(
        Long ingredientId,
        String ingredientName,
        String unit,
        String warehouse,
        String zone,
        BigDecimal currentStock,
        BigDecimal safetyStock,
        BigDecimal avgDailyConsumption,
        BigDecimal reorderPoint,
        BigDecimal recommendedOrderQty,
        String priority
    ) {
        boolean shouldOrderToday() {
            return recommendedOrderQty != null && recommendedOrderQty.compareTo(BigDecimal.ZERO) > 0;
        }

        int priorityOrder() {
            return switch (priority) {
                case "CRITIC" -> 0;
                case "RIDICAT" -> 1;
                case "MEDIU" -> 2;
                default -> 3;
            };
        }
    }
}
