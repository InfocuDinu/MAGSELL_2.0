package com.bakerymanager.service;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.IngredientLocationStock;
import com.bakerymanager.entity.WarehouseLocation;
import com.bakerymanager.repository.IngredientLocationStockRepository;
import com.bakerymanager.repository.WarehouseLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class MultiLocationInventoryService {

    private static final String DEFAULT_WAREHOUSE = "Depozit Principal";
    private static final String DEFAULT_ZONE = "General";

    private final WarehouseLocationRepository warehouseLocationRepository;
    private final IngredientLocationStockRepository ingredientLocationStockRepository;
    private final StockService stockService;

    public MultiLocationInventoryService(WarehouseLocationRepository warehouseLocationRepository,
                                         IngredientLocationStockRepository ingredientLocationStockRepository,
                                         StockService stockService) {
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.ingredientLocationStockRepository = ingredientLocationStockRepository;
        this.stockService = stockService;
    }

    public void syncIngredientLocationStock(Ingredient ingredient) {
        if (ingredient == null || ingredient.getId() == null) {
            return;
        }

        WarehouseLocation primaryLocation = resolvePrimaryLocation(ingredient);
        List<IngredientLocationStock> rows = ingredientLocationStockRepository.findByIngredientId(ingredient.getId());

        if (rows.isEmpty()) {
            IngredientLocationStock seed = new IngredientLocationStock();
            seed.setIngredient(ingredient);
            seed.setLocation(primaryLocation);
            seed.setQuantity(nz(ingredient.getCurrentStock()));
            ingredientLocationStockRepository.save(seed);
            return;
        }

        BigDecimal totalByLocations = ingredientLocationStockRepository.sumQuantityByIngredientId(ingredient.getId());
        BigDecimal expectedTotal = nz(ingredient.getCurrentStock());
        BigDecimal drift = expectedTotal.subtract(nz(totalByLocations));
        if (drift.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        IngredientLocationStock primaryStock = ingredientLocationStockRepository
            .findByIngredientIdAndLocationId(ingredient.getId(), primaryLocation.getId())
            .orElseGet(() -> createLocationStock(ingredient, primaryLocation, BigDecimal.ZERO));

        BigDecimal adjusted = nz(primaryStock.getQuantity()).add(drift);
        if (adjusted.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Stocul rezultat pentru locația principală nu poate fi negativ");
        }
        primaryStock.setQuantity(adjusted);
        ingredientLocationStockRepository.save(primaryStock);
    }

    public void applyDeltaToPrimaryLocation(Ingredient ingredient, BigDecimal delta) {
        if (ingredient == null || ingredient.getId() == null || delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        syncIngredientLocationStock(ingredient);

        WarehouseLocation primaryLocation = resolvePrimaryLocation(ingredient);
        IngredientLocationStock stock = ingredientLocationStockRepository
            .findByIngredientIdAndLocationId(ingredient.getId(), primaryLocation.getId())
            .orElseGet(() -> createLocationStock(ingredient, primaryLocation, BigDecimal.ZERO));

        BigDecimal next = nz(stock.getQuantity()).add(delta);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Stoc insuficient în locația principală pentru articolul " + ingredient.getName());
        }

        stock.setQuantity(next);
        ingredientLocationStockRepository.save(stock);
    }

    public void transferBetweenLocations(Ingredient ingredient,
                                         BigDecimal quantity,
                                         String unit,
                                         String fromWarehouse,
                                         String fromZone,
                                         String toWarehouse,
                                         String toZone,
                                         String reason) {
        if (ingredient == null || ingredient.getId() == null) {
            throw new IllegalArgumentException("Ingredient invalid pentru transfer");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cantitatea transferată trebuie să fie mai mare decât zero");
        }

        syncIngredientLocationStock(ingredient);

        WarehouseLocation from = resolveLocation(fromWarehouse, fromZone, true);
        WarehouseLocation to = resolveLocation(toWarehouse, toZone, true);

        if (from.getLocationCode().equalsIgnoreCase(to.getLocationCode())) {
            throw new IllegalArgumentException("Locația sursă și destinație trebuie să fie diferite");
        }

        IngredientLocationStock sourceStock = ingredientLocationStockRepository
            .findByIngredientIdAndLocationId(ingredient.getId(), from.getId())
            .orElseThrow(() -> new IllegalStateException("Nu există stoc în locația sursă: " + formatLocation(from)));

        BigDecimal sourceQty = nz(sourceStock.getQuantity());
        if (sourceQty.compareTo(quantity) < 0) {
            throw new IllegalStateException("Stoc insuficient în locația sursă. Disponibil=" + sourceQty + ", cerut=" + quantity);
        }

        IngredientLocationStock destinationStock = ingredientLocationStockRepository
            .findByIngredientIdAndLocationId(ingredient.getId(), to.getId())
            .orElseGet(() -> createLocationStock(ingredient, to, BigDecimal.ZERO));

        sourceStock.setQuantity(sourceQty.subtract(quantity));
        destinationStock.setQuantity(nz(destinationStock.getQuantity()).add(quantity));

        ingredientLocationStockRepository.save(sourceStock);
        ingredientLocationStockRepository.save(destinationStock);

        stockService.transferInternal(
            ingredient,
            quantity,
            unit,
            formatLocation(from),
            formatLocation(to),
            reason
        );
    }

    @Transactional(readOnly = true)
    public List<LocationStockRow> buildLocationStockRows() {
        return ingredientLocationStockRepository.findAllWithIngredientAndLocation().stream()
            .map(stock -> new LocationStockRow(
                stock.getIngredient().getName(),
                stock.getLocation().getWarehouseName(),
                stock.getLocation().getZoneName(),
                nz(stock.getQuantity()),
                stock.getIngredient().getUnitOfMeasure() != null ? stock.getIngredient().getUnitOfMeasure().name() : ""
            ))
            .sorted(Comparator
                .comparing(LocationStockRow::ingredientName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(LocationStockRow::warehouse, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(row -> row.zone() != null ? row.zone() : "", String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private WarehouseLocation resolvePrimaryLocation(Ingredient ingredient) {
        return resolveLocation(ingredient.getWarehouse(), ingredient.getZone(), true);
    }

    private WarehouseLocation resolveLocation(String warehouse, String zone, boolean autoCreate) {
        String normalizedWarehouse = normalizeWarehouse(warehouse);
        String normalizedZone = normalizeZone(zone);
        String code = buildLocationCode(normalizedWarehouse, normalizedZone);

        return warehouseLocationRepository.findByLocationCode(code)
            .orElseGet(() -> {
                if (!autoCreate) {
                    throw new IllegalStateException("Locație inexistentă: " + code);
                }
                WarehouseLocation location = new WarehouseLocation();
                location.setWarehouseName(normalizedWarehouse);
                location.setZoneName(normalizedZone);
                location.setLocationCode(code);
                location.setActive(true);
                return warehouseLocationRepository.save(location);
            });
    }

    private IngredientLocationStock createLocationStock(Ingredient ingredient, WarehouseLocation location, BigDecimal quantity) {
        IngredientLocationStock stock = new IngredientLocationStock();
        stock.setIngredient(ingredient);
        stock.setLocation(location);
        stock.setQuantity(nz(quantity));
        return ingredientLocationStockRepository.save(stock);
    }

    private String normalizeWarehouse(String warehouse) {
        if (warehouse == null || warehouse.trim().isEmpty()) {
            return DEFAULT_WAREHOUSE;
        }
        return warehouse.trim();
    }

    private String normalizeZone(String zone) {
        if (zone == null || zone.trim().isEmpty()) {
            return DEFAULT_ZONE;
        }
        return zone.trim();
    }

    private String buildLocationCode(String warehouse, String zone) {
        return (warehouse + "::" + zone).toLowerCase(Locale.ROOT);
    }

    private String formatLocation(WarehouseLocation location) {
        String warehouse = location.getWarehouseName() != null ? location.getWarehouseName() : DEFAULT_WAREHOUSE;
        String zone = location.getZoneName() != null ? location.getZoneName() : DEFAULT_ZONE;
        return warehouse + " / " + zone;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public record LocationStockRow(String ingredientName,
                                   String warehouse,
                                   String zone,
                                   BigDecimal quantity,
                                   String unit) {
    }
}
