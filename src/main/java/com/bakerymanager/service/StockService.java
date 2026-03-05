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
    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final AccessAuditService accessAuditService;

    public StockService(IngredientBatchRepository ingredientBatchRepository,
                        StockMovementRepository stockMovementRepository,
                        UnitConversionService unitConversionService,
                        UserService userService,
                        AuthorizationService authorizationService,
                        AccessAuditService accessAuditService) {
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.unitConversionService = unitConversionService;
        this.userService = userService;
        this.authorizationService = authorizationService;
        this.accessAuditService = accessAuditService;
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
        return receiveBatch(ingredient, quantity, unit, expiryDate, receivedDate, batchCode, sourceType, sourceId, null);
    }

    @Transactional
    public IngredientBatch receiveBatch(Ingredient ingredient,
                                        BigDecimal quantity,
                                        String unit,
                                        LocalDate expiryDate,
                                        LocalDateTime receivedDate,
                                        String batchCode,
                                        String sourceType,
                                        Long sourceId,
                                        String reason) {
        authorizationService.requireOperatorOrAbove("STOCK_RECEIVE", ingredient != null ? ingredient.getName() : "UNKNOWN");
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

        accessAuditService.logDataChange(
            userService.getCurrentUser().orElse(null),
            "STOCK_RECEIVE_BATCH",
            ingredient != null ? ingredient.getName() : "ingredient.unknown",
            "batch=NEW",
            "batch=" + saved.getBatchCode() + ", qty=" + saved.getQuantity(),
            "Recepție lot nou"
        );

        stockMovementRepository.save(createMovement(
            ingredient,
            saved,
            StockMovement.MovementType.RECEIPT,
            normalizedQty,
            targetUnit,
            sourceType,
            sourceId,
            reason,
            null
        ));

        return saved;
    }

    @Transactional
    public List<StockMovement> consumeFefo(Ingredient ingredient,
                                          BigDecimal quantityNeeded,
                                          String unit,
                                          String sourceType,
                                          Long sourceId) {
        return consumeFefo(ingredient, quantityNeeded, unit, sourceType, sourceId, null);
    }

    @Transactional
    public List<StockMovement> consumeFefo(Ingredient ingredient,
                                          BigDecimal quantityNeeded,
                                          String unit,
                                          String sourceType,
                                          Long sourceId,
                                          String reason) {
        authorizationService.requireOperatorOrAbove("STOCK_CONSUME_FEFO", ingredient != null ? ingredient.getName() : "UNKNOWN");
        return consumeFromBatches(
            ingredient,
            quantityNeeded,
            unit,
            sourceType,
            sourceId,
            StockMovement.MovementType.CONSUMPTION,
            reason,
            null
        );
    }

    @Transactional
    public StockMovement consumeFromSpecificBatch(Ingredient ingredient,
                                                  String batchCode,
                                                  BigDecimal quantityNeeded,
                                                  String unit,
                                                  String sourceType,
                                                  Long sourceId,
                                                  String reason) {
        authorizationService.requireOperatorOrAbove("STOCK_CONSUME_BATCH", ingredient != null ? ingredient.getName() : "UNKNOWN");
        validatePositiveQuantity(quantityNeeded);
        if (ingredient == null || ingredient.getId() == null) {
            throw new IllegalArgumentException("Ingredient is required");
        }
        if (batchCode == null || batchCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch code is required");
        }

        String targetUnit = resolveUnit(unit, ingredient);
        BigDecimal normalizedNeeded = unitConversionService.convert(quantityNeeded, unit, targetUnit);

        IngredientBatch batch = ingredientBatchRepository
            .findAvailableBatchByIngredientAndCode(ingredient.getId(), batchCode.trim())
            .orElseThrow(() -> new IllegalStateException(
                "Batch not found or depleted for ingredient " + ingredient.getName() + ": " + batchCode));

        BigDecimal available = batch.getQuantity() != null ? batch.getQuantity() : BigDecimal.ZERO;
        BigDecimal before = available;
        if (available.compareTo(normalizedNeeded) < 0) {
            throw new IllegalStateException("Insufficient stock in batch " + batchCode + ". Missing: " +
                normalizedNeeded.subtract(available));
        }

        batch.setQuantity(available.subtract(normalizedNeeded));
        ingredientBatchRepository.save(batch);

        accessAuditService.logDataChange(
            userService.getCurrentUser().orElse(null),
            "STOCK_CONSUME_BATCH",
            ingredient != null ? ingredient.getName() : "ingredient.unknown",
            "batch=" + batch.getBatchCode() + ", qty=" + before,
            "batch=" + batch.getBatchCode() + ", qty=" + batch.getQuantity(),
            "Consum specific lot"
        );

        StockMovement movement = createMovement(
            ingredient,
            batch,
            StockMovement.MovementType.CONSUMPTION,
            normalizedNeeded,
            targetUnit,
            sourceType,
            sourceId,
            reason,
            "Consum lot scanat: " + batch.getBatchCode()
        );

        return stockMovementRepository.save(movement);
    }

    @Transactional
    public StockMovement transferInternal(Ingredient ingredient,
                                          BigDecimal quantity,
                                          String unit,
                                          String fromLocation,
                                          String toLocation,
                                          String reason) {
        authorizationService.requireAnyRole("STOCK_TRANSFER_INTERNAL", ingredient != null ? ingredient.getName() : "UNKNOWN",
            com.bakerymanager.entity.User.Role.ADMIN,
            com.bakerymanager.entity.User.Role.MANAGER);
        validatePositiveQuantity(quantity);
        String targetUnit = resolveUnit(unit, ingredient);
        BigDecimal normalizedQty = unitConversionService.convert(quantity, unit, targetUnit);

        String source = fromLocation != null && !fromLocation.isBlank() ? fromLocation.trim() : "N/A";
        String destination = toLocation != null && !toLocation.isBlank() ? toLocation.trim() : "N/A";
        String notes = "Transfer intern: " + source + " -> " + destination;

        StockMovement movement = createMovement(
            ingredient,
            null,
            StockMovement.MovementType.TRANSFER,
            normalizedQty,
            targetUnit,
            "INTERNAL_TRANSFER",
            ingredient != null ? ingredient.getId() : null,
            reason,
            notes
        );
        accessAuditService.logBusinessEvent(
            userService.getCurrentUser().orElse(null),
            "STOCK_TRANSFER_INTERNAL",
            ingredient != null ? ingredient.getName() : "ingredient.unknown",
            "Transfer " + source + " -> " + destination + ", qty=" + normalizedQty
        );
        return stockMovementRepository.save(movement);
    }

    @Transactional
    public List<StockMovement> adjustStock(Ingredient ingredient,
                                           BigDecimal quantityDelta,
                                           String unit,
                                           String reason) {
        authorizationService.requireAnyRole("STOCK_ADJUST", ingredient != null ? ingredient.getName() : "UNKNOWN",
            com.bakerymanager.entity.User.Role.ADMIN,
            com.bakerymanager.entity.User.Role.MANAGER);
        if (quantityDelta == null || quantityDelta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Quantity delta must be different from zero");
        }

        if (quantityDelta.compareTo(BigDecimal.ZERO) > 0) {
            return List.of(addStockByAdjustment(ingredient, quantityDelta, unit, reason));
        }

        BigDecimal quantityToDecrease = quantityDelta.abs();
        return consumeFromBatches(
            ingredient,
            quantityToDecrease,
            unit,
            "STOCK_ADJUSTMENT",
            ingredient != null ? ingredient.getId() : null,
            StockMovement.MovementType.ADJUSTMENT,
            reason,
            "Ajustare negativă stoc"
        );
    }

    @Transactional
    public List<StockMovement> returnToSupplier(Ingredient ingredient,
                                                BigDecimal quantity,
                                                String unit,
                                                String reason) {
        authorizationService.requireAnyRole("STOCK_RETURN_SUPPLIER", ingredient != null ? ingredient.getName() : "UNKNOWN",
            com.bakerymanager.entity.User.Role.ADMIN,
            com.bakerymanager.entity.User.Role.MANAGER);
        validatePositiveQuantity(quantity);
        return consumeFromBatches(
            ingredient,
            quantity,
            unit,
            "SUPPLIER_RETURN",
            ingredient != null ? ingredient.getId() : null,
            StockMovement.MovementType.RETURN,
            reason,
            "Retur către furnizor"
        );
    }

    @Transactional
    public List<StockMovement> registerWaste(Ingredient ingredient,
                                             BigDecimal quantity,
                                             String unit,
                                             String reason) {
        authorizationService.requireAnyRole("STOCK_REGISTER_WASTE", ingredient != null ? ingredient.getName() : "UNKNOWN",
            com.bakerymanager.entity.User.Role.ADMIN,
            com.bakerymanager.entity.User.Role.MANAGER);
        validatePositiveQuantity(quantity);
        return consumeFromBatches(
            ingredient,
            quantity,
            unit,
            "STOCK_WASTE",
            ingredient != null ? ingredient.getId() : null,
            StockMovement.MovementType.WASTE,
            reason,
            "Pierdere/Casare"
        );
    }

    private StockMovement addStockByAdjustment(Ingredient ingredient,
                                               BigDecimal quantity,
                                               String unit,
                                               String reason) {
        String targetUnit = resolveUnit(unit, ingredient);
        BigDecimal normalizedQty = unitConversionService.convert(quantity, unit, targetUnit);

        IngredientBatch batch = new IngredientBatch();
        batch.setIngredient(ingredient);
        batch.setQuantity(normalizedQty);
        batch.setReceivedDate(LocalDateTime.now());
        batch.setBatchCode("ADJ-" + System.currentTimeMillis());
        batch.setSourceType("STOCK_ADJUSTMENT");
        batch.setSourceId(ingredient != null ? ingredient.getId() : null);
        IngredientBatch savedBatch = ingredientBatchRepository.save(batch);

        StockMovement movement = createMovement(
            ingredient,
            savedBatch,
            StockMovement.MovementType.ADJUSTMENT,
            normalizedQty,
            targetUnit,
            "STOCK_ADJUSTMENT",
            ingredient != null ? ingredient.getId() : null,
            reason,
            "Ajustare pozitivă stoc"
        );
        return stockMovementRepository.save(movement);
    }

    private List<StockMovement> consumeFromBatches(Ingredient ingredient,
                                                   BigDecimal quantityNeeded,
                                                   String unit,
                                                   String sourceType,
                                                   Long sourceId,
                                                   StockMovement.MovementType movementType,
                                                   String reason,
                                                   String notes) {
        String targetUnit = resolveUnit(unit, ingredient);
        BigDecimal normalizedNeeded = unitConversionService.convert(quantityNeeded, unit, targetUnit);
        List<IngredientBatch> batches = ingredientBatchRepository
                .findAvailableBatchesForIngredient(ingredient.getId());

        batches = ensureBatchCoverageForLegacyStock(ingredient, batches, targetUnit, normalizedNeeded);

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

            BigDecimal before = available;
            BigDecimal used = available.min(remaining);
            batch.setQuantity(available.subtract(used));
            ingredientBatchRepository.save(batch);

            accessAuditService.logDataChange(
                userService.getCurrentUser().orElse(null),
                "STOCK_BATCH_" + movementType.name(),
                ingredient != null ? ingredient.getName() : "ingredient.unknown",
                "batch=" + batch.getBatchCode() + ", qty=" + before,
                "batch=" + batch.getBatchCode() + ", qty=" + batch.getQuantity(),
                "Mișcare " + movementType.name() + ", consum=" + used
            );

            StockMovement movement = createMovement(
                ingredient,
                batch,
                movementType,
                used,
                targetUnit,
                sourceType,
                sourceId,
                reason,
                notes
            );
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

    private List<IngredientBatch> ensureBatchCoverageForLegacyStock(Ingredient ingredient,
                                                                    List<IngredientBatch> batches,
                                                                    String targetUnit,
                                                                    BigDecimal requiredQuantity) {
        if (ingredient == null) {
            return batches;
        }

        BigDecimal ingredientStock = ingredient.getCurrentStock() != null ? ingredient.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal trackedBatchStock = batches == null
            ? BigDecimal.ZERO
            : batches.stream()
                .map(IngredientBatch::getQuantity)
                .filter(q -> q != null && q.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal missingBatchStock = ingredientStock.subtract(trackedBatchStock);

        if (missingBatchStock.compareTo(BigDecimal.ZERO) > 0
            && requiredQuantity != null
            && requiredQuantity.compareTo(trackedBatchStock) > 0) {
            IngredientBatch legacyBatch = new IngredientBatch();
            legacyBatch.setIngredient(ingredient);
            legacyBatch.setQuantity(missingBatchStock);
            legacyBatch.setReceivedDate(LocalDateTime.now());
            legacyBatch.setBatchCode("LEGACY-" + System.currentTimeMillis());
            legacyBatch.setSourceType("LEGACY_STOCK_RECONCILE");
            legacyBatch.setSourceId(ingredient.getId());

            ingredientBatchRepository.save(legacyBatch);

            return ingredientBatchRepository.findAvailableBatchesForIngredient(ingredient.getId());
        }

        return batches;
    }

    private StockMovement createMovement(Ingredient ingredient,
                                         IngredientBatch batch,
                                         StockMovement.MovementType movementType,
                                         BigDecimal quantity,
                                         String unit,
                                         String sourceType,
                                         Long sourceId,
                                         String reason,
                                         String notes) {
        StockMovement movement = new StockMovement();
        movement.setIngredient(ingredient);
        movement.setBatch(batch);
        movement.setMovementType(movementType);
        movement.setQuantity(quantity != null ? quantity : BigDecimal.ZERO);
        movement.setUnit(unit);
        movement.setMovementDate(LocalDateTime.now());
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setReason(resolveReason(reason, movementType));
        movement.setPerformedByUser(resolveCurrentUser());
        movement.setNotes(notes);
        return movement;
    }

    private String resolveReason(String reason, StockMovement.MovementType movementType) {
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }
        return switch (movementType) {
            case RECEIPT -> "Recepție stoc";
            case CONSUMPTION -> "Consum stoc";
            case ADJUSTMENT -> "Ajustare stoc";
            case WASTE -> "Pierdere/Casare";
            case RETURN -> "Retur furnizor";
            case TRANSFER -> "Transfer intern";
        };
    }

    private String resolveCurrentUser() {
        return userService.getCurrentUser()
            .map(user -> user.getUsername() != null ? user.getUsername() : user.getFullName())
            .orElse("SYSTEM");
    }

    private void validatePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
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
