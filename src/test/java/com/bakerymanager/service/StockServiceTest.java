package com.bakerymanager.service;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.IngredientBatch;
import com.bakerymanager.entity.StockMovement;
import com.bakerymanager.repository.IngredientBatchRepository;
import com.bakerymanager.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private IngredientBatchRepository ingredientBatchRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private UnitConversionService unitConversionService;

    @Mock
    private UserService userService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private AccessAuditService accessAuditService;

    @Test
    void shouldConsumeFromSpecificBatchWhenBarcodeOuttakeUsesLot() {
        StockService service = new StockService(
            ingredientBatchRepository,
            stockMovementRepository,
            unitConversionService,
            userService,
            authorizationService,
            accessAuditService
        );

        Ingredient ingredient = new Ingredient();
        ingredient.setId(10L);
        ingredient.setName("Făină");
        ingredient.setUnitOfMeasure(Ingredient.UnitOfMeasure.KG);

        IngredientBatch batch = new IngredientBatch();
        batch.setId(100L);
        batch.setIngredient(ingredient);
        batch.setBatchCode("LOT-FAINA-01");
        batch.setQuantity(new BigDecimal("10.000"));

        when(unitConversionService.convert(any(BigDecimal.class), any(String.class), any(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(ingredientBatchRepository.findAvailableBatchByIngredientAndCode(10L, "LOT-FAINA-01"))
            .thenReturn(Optional.of(batch));
        when(ingredientBatchRepository.save(any(IngredientBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement movement = service.consumeFromSpecificBatch(
            ingredient,
            "LOT-FAINA-01",
            new BigDecimal("3.000"),
            "KG",
            "BARCODE_SCANNER_OUTTAKE",
            10L,
            "Consum scanat"
        );

        assertEquals(new BigDecimal("7.000"), batch.getQuantity());
        assertEquals(StockMovement.MovementType.CONSUMPTION, movement.getMovementType());
        assertEquals("BARCODE_SCANNER_OUTTAKE", movement.getSourceType());
        assertEquals("Consum lot scanat: LOT-FAINA-01", movement.getNotes());
    }

    @Test
    void shouldFailWhenSpecificBatchHasInsufficientStock() {
        StockService service = new StockService(
            ingredientBatchRepository,
            stockMovementRepository,
            unitConversionService,
            userService,
            authorizationService,
            accessAuditService
        );

        Ingredient ingredient = new Ingredient();
        ingredient.setId(11L);
        ingredient.setName("Zahăr");
        ingredient.setUnitOfMeasure(Ingredient.UnitOfMeasure.KG);

        IngredientBatch batch = new IngredientBatch();
        batch.setIngredient(ingredient);
        batch.setBatchCode("LOT-ZAHAR-01");
        batch.setQuantity(new BigDecimal("1.000"));

        when(unitConversionService.convert(any(BigDecimal.class), any(String.class), any(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(ingredientBatchRepository.findAvailableBatchByIngredientAndCode(11L, "LOT-ZAHAR-01"))
            .thenReturn(Optional.of(batch));

        assertThrows(IllegalStateException.class, () -> service.consumeFromSpecificBatch(
            ingredient,
            "LOT-ZAHAR-01",
            new BigDecimal("2.000"),
            "KG",
            "BARCODE_SCANNER_OUTTAKE",
            11L,
            "Consum scanat"
        ));
    }
}
