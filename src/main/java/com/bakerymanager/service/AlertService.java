package com.bakerymanager.service;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.IngredientBatch;
import com.bakerymanager.entity.ProductionConsumption;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.SaleItem;
import com.bakerymanager.repository.IngredientBatchRepository;
import com.bakerymanager.repository.ProductionConsumptionRepository;
import com.bakerymanager.repository.RecipeItemRepository;
import com.bakerymanager.repository.SaleItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    private static final int EXPIRY_WINDOW_DAYS = 7;
    private static final BigDecimal CONSUMPTION_VARIANCE_THRESHOLD = BigDecimal.valueOf(0.10); // 10%

    private final IngredientService ingredientService;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final ProductionConsumptionRepository productionConsumptionRepository;
    private final SaleItemRepository saleItemRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final UnitConversionService unitConversionService;

    public AlertService(IngredientService ingredientService,
                        IngredientBatchRepository ingredientBatchRepository,
                        ProductionConsumptionRepository productionConsumptionRepository,
                        SaleItemRepository saleItemRepository,
                        RecipeItemRepository recipeItemRepository,
                        UnitConversionService unitConversionService) {
        this.ingredientService = ingredientService;
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.productionConsumptionRepository = productionConsumptionRepository;
        this.saleItemRepository = saleItemRepository;
        this.recipeItemRepository = recipeItemRepository;
        this.unitConversionService = unitConversionService;
    }

    public List<Alert> getAlerts(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<Alert> alerts = new ArrayList<>();

        for (Ingredient ingredient : ingredientService.getLowStockIngredients()) {
            alerts.add(new Alert(
                now,
                "Stoc scăzut",
                String.format("%s: %s / min %s", ingredient.getName(), ingredient.getCurrentStock(), ingredient.getMinimumStock()),
                Alert.Severity.WARNING
            ));
        }

        List<IngredientBatch> expiring = ingredientBatchRepository
            .findByExpiryDateBetween(LocalDate.now(), LocalDate.now().plusDays(EXPIRY_WINDOW_DAYS));
        for (IngredientBatch batch : expiring) {
            String ingredientName = batch.getIngredient() != null ? batch.getIngredient().getName() : "";
            alerts.add(new Alert(
                now,
                "Lot aproape expirat",
                String.format("%s | lot %s | expiră %s", ingredientName,
                    batch.getBatchCode() != null ? batch.getBatchCode() : "-",
                    batch.getExpiryDate()),
                Alert.Severity.WARNING
            ));
        }

        List<ProductionConsumption> consumptions = productionConsumptionRepository.findByPlannedDateBetween(start, end);
        for (ProductionConsumption consumption : consumptions) {
            BigDecimal standard = consumption.getStandardQuantity() != null ? consumption.getStandardQuantity() : BigDecimal.ZERO;
            BigDecimal actual = consumption.getActualQuantity() != null ? consumption.getActualQuantity() : BigDecimal.ZERO;
            if (standard.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal varianceRatio = actual.subtract(standard)
                .divide(standard, 4, RoundingMode.HALF_UP)
                .abs();
            if (varianceRatio.compareTo(CONSUMPTION_VARIANCE_THRESHOLD) > 0) {
                String ingredientName = consumption.getIngredient() != null ? consumption.getIngredient().getName() : "";
                alerts.add(new Alert(
                    now,
                    "Consum deviat",
                    String.format("%s: std %s, real %s", ingredientName, standard, actual),
                    Alert.Severity.WARNING
                ));
            }
        }

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay().minusSeconds(1);
        List<SaleItem> saleItems = saleItemRepository.findBySaleDateBetween(startDateTime, endDateTime);
        for (SaleItem item : saleItems) {
            Product product = item.getProduct();
            if (product == null) {
                continue;
            }
            BigDecimal unitCost = calculateUnitCost(product);
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            if (unitPrice.compareTo(unitCost) < 0) {
                alerts.add(new Alert(
                    now,
                    "Marjă negativă",
                    String.format("%s: preț %s < cost %s", product.getName(), unitPrice, unitCost),
                    Alert.Severity.CRITICAL
                ));
            }
        }

        return alerts;
    }

    private BigDecimal calculateUnitCost(Product product) {
        List<RecipeItem> recipeItems = recipeItemRepository.findByProductWithIngredient(product);
        if (recipeItems == null || recipeItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (RecipeItem item : recipeItems) {
            BigDecimal required = item.getRequiredQuantity() != null ? item.getRequiredQuantity() : BigDecimal.ZERO;
            BigDecimal price = item.getIngredient() != null && item.getIngredient().getLastPurchasePrice() != null
                ? item.getIngredient().getLastPurchasePrice()
                : BigDecimal.ZERO;
            BigDecimal normalizedRequired = toIngredientUnit(item, required);
            total = total.add(normalizedRequired.multiply(price));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toIngredientUnit(RecipeItem recipeItem, BigDecimal quantity) {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        if (recipeItem == null || recipeItem.getIngredient() == null) {
            return quantity;
        }
        String ingredientUnit = recipeItem.getIngredient().getUnitOfMeasure() != null
            ? recipeItem.getIngredient().getUnitOfMeasure().name()
            : null;
        String recipeUnit = recipeItem.getUnit();
        if (ingredientUnit == null || ingredientUnit.isBlank() || recipeUnit == null || recipeUnit.isBlank()) {
            return quantity;
        }
        return unitConversionService.convert(quantity, recipeUnit, ingredientUnit);
    }
}
