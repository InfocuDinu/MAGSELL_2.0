package com.bakerymanager.service;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.IngredientLocationStock;
import com.bakerymanager.entity.WarehouseLocation;
import com.bakerymanager.repository.IngredientLocationStockRepository;
import com.bakerymanager.repository.WarehouseLocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiLocationInventoryServiceTest {

    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;

    @Mock
    private IngredientLocationStockRepository ingredientLocationStockRepository;

    @Mock
    private StockService stockService;

    @Test
    void shouldSeedLocationStockWhenIngredientHasNoLocationRows() {
        MultiLocationInventoryService service = new MultiLocationInventoryService(
            warehouseLocationRepository,
            ingredientLocationStockRepository,
            stockService
        );

        Ingredient ingredient = new Ingredient();
        ingredient.setId(1L);
        ingredient.setName("Făină");
        ingredient.setCurrentStock(new BigDecimal("12.500"));
        ingredient.setWarehouse("Depozit Central");
        ingredient.setZone("Raft A");

        WarehouseLocation location = new WarehouseLocation();
        location.setId(100L);
        location.setWarehouseName("Depozit Central");
        location.setZoneName("Raft A");
        location.setLocationCode("depozit central::raft a");

        when(warehouseLocationRepository.findByLocationCode("depozit central::raft a")).thenReturn(Optional.of(location));
        when(ingredientLocationStockRepository.findByIngredientId(1L)).thenReturn(List.of());
        when(ingredientLocationStockRepository.save(any(IngredientLocationStock.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.syncIngredientLocationStock(ingredient);

        verify(ingredientLocationStockRepository).save(any(IngredientLocationStock.class));
    }

    @Test
    void shouldTransferStockBetweenLocationsAndLogMovement() {
        MultiLocationInventoryService service = new MultiLocationInventoryService(
            warehouseLocationRepository,
            ingredientLocationStockRepository,
            stockService
        );

        Ingredient ingredient = new Ingredient();
        ingredient.setId(2L);
        ingredient.setName("Zahăr");
        ingredient.setCurrentStock(new BigDecimal("10.000"));
        ingredient.setWarehouse("Depozit A");
        ingredient.setZone("Z1");
        ingredient.setUnitOfMeasure(Ingredient.UnitOfMeasure.KG);

        WarehouseLocation from = new WarehouseLocation();
        from.setId(201L);
        from.setWarehouseName("Depozit A");
        from.setZoneName("Z1");
        from.setLocationCode("depozit a::z1");

        WarehouseLocation to = new WarehouseLocation();
        to.setId(202L);
        to.setWarehouseName("Depozit B");
        to.setZoneName("Z2");
        to.setLocationCode("depozit b::z2");

        IngredientLocationStock fromStock = new IngredientLocationStock();
        fromStock.setIngredient(ingredient);
        fromStock.setLocation(from);
        fromStock.setQuantity(new BigDecimal("6.000"));

        IngredientLocationStock toStock = new IngredientLocationStock();
        toStock.setIngredient(ingredient);
        toStock.setLocation(to);
        toStock.setQuantity(new BigDecimal("1.000"));

        when(warehouseLocationRepository.findByLocationCode("depozit a::z1")).thenReturn(Optional.of(from));
        when(warehouseLocationRepository.findByLocationCode("depozit b::z2")).thenReturn(Optional.of(to));

        when(ingredientLocationStockRepository.findByIngredientId(2L)).thenReturn(List.of(fromStock, toStock));
        when(ingredientLocationStockRepository.sumQuantityByIngredientId(2L)).thenReturn(new BigDecimal("10.000"));

        when(ingredientLocationStockRepository.findByIngredientIdAndLocationId(2L, 201L)).thenReturn(Optional.of(fromStock));
        when(ingredientLocationStockRepository.findByIngredientIdAndLocationId(2L, 202L)).thenReturn(Optional.of(toStock));
        when(ingredientLocationStockRepository.save(any(IngredientLocationStock.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.transferBetweenLocations(
            ingredient,
            new BigDecimal("2.500"),
            "KG",
            "Depozit A",
            "Z1",
            "Depozit B",
            "Z2",
            "Transfer operațional"
        );

        assertEquals(new BigDecimal("3.500"), fromStock.getQuantity());
        assertEquals(new BigDecimal("3.500"), toStock.getQuantity());
        verify(stockService).transferInternal(
            ingredient,
            new BigDecimal("2.500"),
            "KG",
            "Depozit A / Z1",
            "Depozit B / Z2",
            "Transfer operațional"
        );
    }

    @Test
    void shouldRejectTransferWhenSourceAndDestinationAreSame() {
        MultiLocationInventoryService service = new MultiLocationInventoryService(
            warehouseLocationRepository,
            ingredientLocationStockRepository,
            stockService
        );

        Ingredient ingredient = new Ingredient();
        ingredient.setId(3L);
        ingredient.setName("Sare");
        ingredient.setCurrentStock(new BigDecimal("5.000"));
        ingredient.setWarehouse("Depozit A");
        ingredient.setZone("Z1");

        WarehouseLocation sameLocation = new WarehouseLocation();
        sameLocation.setId(301L);
        sameLocation.setWarehouseName("Depozit A");
        sameLocation.setZoneName("Z1");
        sameLocation.setLocationCode("depozit a::z1");

        when(warehouseLocationRepository.findByLocationCode("depozit a::z1")).thenReturn(Optional.of(sameLocation));
        when(ingredientLocationStockRepository.findByIngredientId(3L)).thenReturn(List.of());
        when(ingredientLocationStockRepository.save(any(IngredientLocationStock.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> service.transferBetweenLocations(
            ingredient,
            new BigDecimal("1.000"),
            "KG",
            "Depozit A",
            "Z1",
            "Depozit A",
            "Z1",
            "Test"
        ));
    }
}
