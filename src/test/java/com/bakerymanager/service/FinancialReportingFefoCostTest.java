package com.bakerymanager.service;

import com.bakerymanager.entity.CostBreakdown;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.IngredientBatch;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.repository.CostBreakdownRepository;
import com.bakerymanager.repository.DailyFinancialReportRepository;
import com.bakerymanager.repository.IngredientBatchRepository;
import com.bakerymanager.repository.ProductionReportRepository;
import com.bakerymanager.repository.RecipeItemRepository;
import com.bakerymanager.repository.SaleItemRepository;
import com.bakerymanager.repository.VATReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialReportingFefoCostTest {

    @Mock
    private CostBreakdownRepository costBreakdownRepository;
    @Mock
    private DailyFinancialReportRepository dailyFinancialReportRepository;
    @Mock
    private VATReportRepository vatReportRepository;
    @Mock
    private ProductionReportRepository productionReportRepository;
    @Mock
    private SaleItemRepository saleItemRepository;
    @Mock
    private RecipeItemRepository recipeItemRepository;
    @Mock
    private IngredientBatchRepository ingredientBatchRepository;
    @Mock
    private UnitConversionService unitConversionService;

    @Test
    void shouldCalculateRawMaterialCostUsingFefoBatches() {
        FinancialReportingService service = new FinancialReportingService(
            costBreakdownRepository,
            dailyFinancialReportRepository,
            vatReportRepository,
            productionReportRepository,
            saleItemRepository,
            recipeItemRepository,
            ingredientBatchRepository,
            unitConversionService
        );

        Product product = new Product();
        product.setName("Pâine" );

        Ingredient ingredient = new Ingredient();
        ingredient.setId(1L);
        ingredient.setName("Făină");
        ingredient.setUnitOfMeasure(Ingredient.UnitOfMeasure.KG);
        ingredient.setLastPurchasePrice(new BigDecimal("6.00"));

        RecipeItem recipeItem = new RecipeItem();
        recipeItem.setProduct(product);
        recipeItem.setIngredient(ingredient);
        recipeItem.setComponentType(RecipeItem.ComponentType.INGREDIENT);
        recipeItem.setRequiredQuantity(new BigDecimal("2.000"));
        recipeItem.setUnit("KG");

        ProductionReport productionReport = new ProductionReport();
        productionReport.setProduct(product);
        productionReport.setQuantityProduced(new BigDecimal("10.000"));
        productionReport.setProductionDate(LocalDateTime.now());

        IngredientBatch b1 = new IngredientBatch();
        b1.setQuantity(new BigDecimal("5.000"));
        b1.setUnitPrice(new BigDecimal("3.00"));

        IngredientBatch b2 = new IngredientBatch();
        b2.setQuantity(new BigDecimal("10.000"));
        b2.setUnitPrice(new BigDecimal("4.00"));

        IngredientBatch b3 = new IngredientBatch();
        b3.setQuantity(new BigDecimal("10.000"));
        b3.setUnitPrice(new BigDecimal("5.00"));

        LocalDate costDate = LocalDate.now();
        LocalDateTime start = costDate.atStartOfDay();
        LocalDateTime end = costDate.plusDays(1).atStartOfDay();

        when(costBreakdownRepository.findByProductAndDate(product, costDate)).thenReturn(Optional.empty());
        when(productionReportRepository.findByProductAndProductionDateBetweenOrderByProductionDateDesc(product, start, end))
            .thenReturn(List.of(productionReport));
        when(recipeItemRepository.findByProductWithIngredient(product)).thenReturn(List.of(recipeItem));
        when(unitConversionService.convert(any(BigDecimal.class), any(String.class), any(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(ingredientBatchRepository.findAvailableBatchesForIngredientAsOf(any(Long.class), any(LocalDateTime.class)))
            .thenReturn(List.of(b1, b2, b3));
        when(costBreakdownRepository.save(any(CostBreakdown.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CostBreakdown result = service.calculateProductionCost(product, costDate);

        assertEquals("FEFO", result.getCostMethod());
        assertEquals(0, result.getRawMaterialCost().compareTo(new BigDecimal("80.0000")));
        assertEquals(0, result.getQuantityProduced().compareTo(new BigDecimal("10.000")));
        assertEquals(0, result.getOverheadCost().compareTo(new BigDecimal("5.0000")));
        assertEquals(0, result.getUnitCost().compareTo(new BigDecimal("8.5000")));
        assertTrue(result.getNotes().contains("FEFO"));
    }
}
