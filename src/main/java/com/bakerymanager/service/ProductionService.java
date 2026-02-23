package com.bakerymanager.service;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.ProductionOrder;
import com.bakerymanager.entity.ProductionOrderLine;
import com.bakerymanager.entity.ProductionConsumption;
import com.bakerymanager.repository.RecipeItemRepository;
import com.bakerymanager.repository.ProductionReportRepository;
import com.bakerymanager.repository.ProductionOrderRepository;
import com.bakerymanager.repository.ProductionOrderLineRepository;
import com.bakerymanager.repository.ProductionConsumptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductionService {
    
    private final RecipeItemRepository recipeItemRepository;
    private final ProductionReportRepository productionReportRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionOrderLineRepository productionOrderLineRepository;
    private final ProductionConsumptionRepository productionConsumptionRepository;
    private final ProductService productService;
    private final IngredientService ingredientService;
    private final StockService stockService;
    private final UnitConversionService unitConversionService;
    
    public ProductionService(RecipeItemRepository recipeItemRepository,
                           ProductionReportRepository productionReportRepository,
                           ProductionOrderRepository productionOrderRepository,
                           ProductionOrderLineRepository productionOrderLineRepository,
                           ProductionConsumptionRepository productionConsumptionRepository,
                           ProductService productService, 
                           IngredientService ingredientService,
                           StockService stockService,
                           UnitConversionService unitConversionService) {
        this.recipeItemRepository = recipeItemRepository;
        this.productionReportRepository = productionReportRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.productionOrderLineRepository = productionOrderLineRepository;
        this.productionConsumptionRepository = productionConsumptionRepository;
        this.productService = productService;
        this.ingredientService = ingredientService;
        this.stockService = stockService;
        this.unitConversionService = unitConversionService;
    }

    public ProductionOrder createProductionOrder(LocalDate plannedDate, String notes) {
        ProductionOrder order = new ProductionOrder();
        if (plannedDate != null) {
            order.setPlannedDate(plannedDate);
        }
        order.setNotes(notes);
        order.setStatus(ProductionOrder.Status.PLANNED);
        return productionOrderRepository.save(order);
    }

    public ProductionOrder addProductionOrderLine(Long productionOrderId, Long productId, BigDecimal plannedQuantity) {
        ProductionOrder order = productionOrderRepository.findById(productionOrderId)
            .orElseThrow(() -> new RuntimeException("Production order not found: " + productionOrderId));
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        ProductionOrderLine line = new ProductionOrderLine();
        line.setProductionOrder(order);
        line.setProduct(product);
        line.setPlannedQuantity(plannedQuantity);
        line.setUnit("BUC");
        productionOrderLineRepository.save(line);

        planStandardConsumption(line.getId());

        order.getLines().add(line);
        return productionOrderRepository.save(order);
    }

    public List<ProductionOrder> getProductionOrders(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? endDate : LocalDate.now().plusDays(7);
        return productionOrderRepository.findByPlannedDateBetweenOrderByPlannedDate(start, end);
    }

    public ProductionOrder getProductionOrderWithLines(Long productionOrderId) {
        return productionOrderRepository.findByIdWithLines(productionOrderId)
            .orElseThrow(() -> new RuntimeException("Production order not found: " + productionOrderId));
    }

    public ProductionOrder startProductionOrder(Long productionOrderId) {
        ProductionOrder order = productionOrderRepository.findById(productionOrderId)
            .orElseThrow(() -> new RuntimeException("Production order not found: " + productionOrderId));
        order.setStatus(ProductionOrder.Status.IN_PROGRESS);
        return productionOrderRepository.save(order);
    }

    public ProductionOrder completeProductionOrder(Long productionOrderId) {
        ProductionOrder order = getProductionOrderWithLines(productionOrderId);
        if (order.getLines() != null) {
            for (ProductionOrderLine line : order.getLines()) {
                if (line.getProduct() != null && line.getPlannedQuantity() != null) {
                    executeProduction(line.getProduct().getId(), line.getPlannedQuantity());
                    line.setActualQuantity(line.getPlannedQuantity());
                    productionOrderLineRepository.save(line);
                    syncActualConsumption(line.getId());
                }
            }
        }
        order.setStatus(ProductionOrder.Status.COMPLETED);
        return productionOrderRepository.save(order);
    }

    public void planStandardConsumption(Long productionOrderLineId) {
        ProductionOrderLine line = productionOrderLineRepository.findById(productionOrderLineId)
            .orElseThrow(() -> new RuntimeException("Production order line not found: " + productionOrderLineId));

        Product product = line.getProduct();
        BigDecimal plannedQuantity = line.getPlannedQuantity();
        List<RecipeItem> recipeItems = recipeItemRepository.findByProductWithIngredient(product);

        for (RecipeItem recipeItem : recipeItems) {
            BigDecimal standardQuantity = toIngredientUnit(recipeItem, recipeItem.getTotalRequiredQuantity(plannedQuantity));
            ProductionConsumption consumption = new ProductionConsumption();
            consumption.setProductionOrderLine(line);
            consumption.setIngredient(recipeItem.getIngredient());
            consumption.setStandardQuantity(standardQuantity);
            consumption.setUnit(resolveIngredientUnit(recipeItem));
            productionConsumptionRepository.save(consumption);
        }
    }

    public void recordActualConsumption(Long productionConsumptionId, BigDecimal actualQuantity) {
        ProductionConsumption consumption = productionConsumptionRepository.findById(productionConsumptionId)
            .orElseThrow(() -> new RuntimeException("Production consumption not found: " + productionConsumptionId));
        consumption.setActualQuantity(actualQuantity);
        productionConsumptionRepository.save(consumption);
    }

    private void syncActualConsumption(Long productionOrderLineId) {
        List<ProductionConsumption> consumptions = productionConsumptionRepository
            .findByProductionOrderLineId(productionOrderLineId);
        for (ProductionConsumption consumption : consumptions) {
            if (consumption.getActualQuantity() == null || consumption.getActualQuantity().compareTo(BigDecimal.ZERO) == 0) {
                consumption.setActualQuantity(consumption.getStandardQuantity());
                productionConsumptionRepository.save(consumption);
            }
        }
    }
    
    public List<RecipeItem> getRecipeByProduct(Long productId) {
        return recipeItemRepository.findByProductIdWithIngredient(productId);
    }
    
    public List<RecipeItem> getRecipeByProduct(Product product) {
        return recipeItemRepository.findByProductWithIngredient(product);
    }
    
    public RecipeItem addRecipeItem(Long productId, Long ingredientId, BigDecimal requiredQuantity) {
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        Ingredient ingredient = ingredientService.getIngredientById(ingredientId)
            .orElseThrow(() -> new RuntimeException("Ingredient not found: " + ingredientId));
        
        Optional<RecipeItem> existingItem = recipeItemRepository.findByProductAndIngredient(product, ingredient);
        String defaultUnit = ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : null;
        if (existingItem.isPresent()) {
            RecipeItem item = existingItem.get();
            item.setRequiredQuantity(requiredQuantity);
            if (item.getUnit() == null || item.getUnit().isBlank()) {
                item.setUnit(defaultUnit);
            }
            return recipeItemRepository.save(item);
        } else {
            RecipeItem newItem = new RecipeItem();
            newItem.setProduct(product);
            newItem.setIngredient(ingredient);
            newItem.setRequiredQuantity(requiredQuantity);
            newItem.setUnit(defaultUnit);
            return recipeItemRepository.save(newItem);
        }
    }
    
    public void removeRecipeItem(Long recipeItemId) {
        recipeItemRepository.deleteById(recipeItemId);
    }
    
    public void removeRecipeItem(Long productId, Long ingredientId) {
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        Ingredient ingredient = ingredientService.getIngredientById(ingredientId)
            .orElseThrow(() -> new RuntimeException("Ingredient not found: " + ingredientId));
        
        recipeItemRepository.findByProductAndIngredient(product, ingredient)
            .ifPresent(item -> recipeItemRepository.delete(item));
    }
    
    public void executeProduction(Long productId, BigDecimal quantity) {
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        List<RecipeItem> recipeItems = recipeItemRepository.findByProductWithIngredient(product);
        if (recipeItems.isEmpty()) {
            throw new RuntimeException("No recipe defined for product: " + product.getName());
        }
        
        Map<Long, BigDecimal> requiredIngredients = recipeItems.stream()
            .collect(Collectors.toMap(
                item -> item.getIngredient().getId(),
                item -> toIngredientUnit(item, item.getTotalRequiredQuantity(quantity))
            ));
        
        // Verify all ingredients have sufficient stock first (atomic check)
        for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
            if (!ingredientService.hasSufficientStock(entry.getKey(), entry.getValue())) {
                Ingredient ingredient = ingredientService.getIngredientById(entry.getKey()).get();
                throw new RuntimeException("Insufficient stock for ingredient: " + ingredient.getName() + 
                    ". Required: " + entry.getValue() + ", Available: " + ingredient.getCurrentStock());
            }
        }
        
        // All checks passed, now remove stock from all ingredients
        for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
            Ingredient ingredient = ingredientService.getIngredientById(entry.getKey()).orElse(null);
            if (ingredient == null) {
                throw new RuntimeException("Ingredient not found: " + entry.getKey());
            }
            ingredientService.removeStock(entry.getKey(), entry.getValue());
            stockService.consumeFefo(
                ingredient,
                entry.getValue(),
                ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : null,
                "PRODUCTION",
                productId
            );
        }
        
        // Add product stock
        productService.addStock(productId, quantity);
        
        // Create production report
        ProductionReport report = new ProductionReport();
        report.setProduct(product);
        report.setQuantityProduced(quantity);
        report.setProductionDate(LocalDateTime.now());
        report.setStatus(ProductionReport.ProductionStatus.COMPLETED);
        productionReportRepository.save(report);
    }
    
    public List<ProductionReport> getAllProductionReports() {
        return productionReportRepository.findAllOrderByProductionDateDesc();
    }
    
    public ProductionReport saveProductionReport(ProductionReport report) {
        return productionReportRepository.save(report);
    }
    
    public List<ProductionReport> getProductionReportsByProduct(Product product) {
        return productionReportRepository.findByProductOrderByProductionDateDesc(product);
    }

    private BigDecimal toIngredientUnit(RecipeItem recipeItem, BigDecimal quantity) {
        String ingredientUnit = resolveIngredientUnit(recipeItem);
        String recipeUnit = recipeItem != null ? recipeItem.getUnit() : null;
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        if (ingredientUnit == null || ingredientUnit.isBlank() || recipeUnit == null || recipeUnit.isBlank()) {
            return quantity;
        }
        return unitConversionService.convert(quantity, recipeUnit, ingredientUnit);
    }

    private String resolveIngredientUnit(RecipeItem recipeItem) {
        if (recipeItem == null || recipeItem.getIngredient() == null) {
            return null;
        }
        return recipeItem.getIngredient().getUnitOfMeasure() != null
            ? recipeItem.getIngredient().getUnitOfMeasure().name()
            : recipeItem.getUnit();
    }
    
    public List<ProductionReport> getProductionReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return productionReportRepository.findByProductionDateBetween(startDate, endDate);
    }
    
    public Map<Ingredient, BigDecimal> calculateRequiredIngredients(Long productId, BigDecimal quantity) {
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        List<RecipeItem> recipeItems = recipeItemRepository.findByProductWithIngredient(product);
        
        return recipeItems.stream()
            .collect(Collectors.toMap(
                RecipeItem::getIngredient,
                item -> toIngredientUnit(item, item.getTotalRequiredQuantity(quantity))
            ));
    }
    
    public boolean canProduce(Long productId, BigDecimal quantity) {
        try {
            Map<Ingredient, BigDecimal> requiredIngredients = calculateRequiredIngredients(productId, quantity);
            for (Map.Entry<Ingredient, BigDecimal> entry : requiredIngredients.entrySet()) {
                if (!ingredientService.hasSufficientStock(entry.getKey().getId(), entry.getValue())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
