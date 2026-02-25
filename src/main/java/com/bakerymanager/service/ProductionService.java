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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        BigDecimal adjustedInputQuantity = adjustForTechnologicalSheet(product, plannedQuantity);
        List<RecipeItem> recipeItems = recipeItemRepository.findByProductWithIngredient(product);

        for (RecipeItem recipeItem : recipeItems) {
            BigDecimal standardQuantity = toIngredientUnit(recipeItem, recipeItem.getTotalRequiredQuantity(adjustedInputQuantity));
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
            item.setComponentType(RecipeItem.ComponentType.INGREDIENT);
            item.setSourceProduct(null);
            return recipeItemRepository.save(item);
        } else {
            RecipeItem newItem = new RecipeItem();
            newItem.setProduct(product);
            newItem.setIngredient(ingredient);
            newItem.setRequiredQuantity(requiredQuantity);
            newItem.setUnit(defaultUnit);
            newItem.setComponentType(RecipeItem.ComponentType.INGREDIENT);
            return recipeItemRepository.save(newItem);
        }
    }

    public RecipeItem addRecipeProductItem(Long productId, Long sourceProductId, BigDecimal requiredQuantity) {
        Product product = productService.getProductById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        Product sourceProduct = productService.getProductById(sourceProductId)
            .orElseThrow(() -> new RuntimeException("Source product not found: " + sourceProductId));

        if (product.getId().equals(sourceProduct.getId())) {
            throw new RuntimeException("Un produs nu se poate consuma pe sine în rețetă.");
        }
        if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Cantitatea pentru semifabricat trebuie să fie > 0.");
        }

        List<RecipeItem> existingRecipe = recipeItemRepository.findByProductWithIngredient(product);
        Optional<RecipeItem> existingItem = existingRecipe.stream()
            .filter(item -> item.getComponentType() == RecipeItem.ComponentType.PRODUCT
                && item.getSourceProduct() != null
                && item.getSourceProduct().getId().equals(sourceProductId))
            .findFirst();

        if (existingItem.isPresent()) {
            RecipeItem item = existingItem.get();
            item.setRequiredQuantity(requiredQuantity);
            if (item.getUnit() == null || item.getUnit().isBlank()) {
                item.setUnit("BUC");
            }
            return recipeItemRepository.save(item);
        }

        RecipeItem newItem = new RecipeItem();
        newItem.setProduct(product);
        newItem.setComponentType(RecipeItem.ComponentType.PRODUCT);
        newItem.setSourceProduct(sourceProduct);
        newItem.setIngredient(null);
        newItem.setRequiredQuantity(requiredQuantity);
        newItem.setUnit("BUC");
        return recipeItemRepository.save(newItem);
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
        executeProductionInternal(productId, quantity, new HashSet<>());
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
        Map<Long, BigDecimal> ingredientRequirements = new HashMap<>();
        Map<Long, BigDecimal> virtualProductStocks = new HashMap<>();
        collectIngredientRequirements(product, quantity, new HashSet<>(), ingredientRequirements, virtualProductStocks);

        return ingredientRequirements.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> ingredientService.getIngredientById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Ingredient not found: " + entry.getKey())),
                Map.Entry::getValue
            ));
    }

    private BigDecimal adjustForTechnologicalSheet(Product product, BigDecimal targetOutputQuantity) {
        if (targetOutputQuantity == null) {
            return BigDecimal.ZERO;
        }
        if (product == null) {
            return targetOutputQuantity;
        }
        BigDecimal multiplier = product.getInputMultiplierForTargetOutput();
        return targetOutputQuantity.multiply(multiplier);
    }
    
    public boolean canProduce(Long productId, BigDecimal quantity) {
        try {
            Map<Long, BigDecimal> ingredientRequirements = new HashMap<>();
            Map<Long, BigDecimal> virtualProductStocks = new HashMap<>();
            Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            collectIngredientRequirements(product, quantity, new HashSet<>(), ingredientRequirements, virtualProductStocks);

            for (Map.Entry<Long, BigDecimal> entry : ingredientRequirements.entrySet()) {
                if (!ingredientService.hasSufficientStock(entry.getKey(), entry.getValue())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void executeProductionInternal(Long productId, BigDecimal quantity, Set<Long> processingPath) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }
        if (processingPath.contains(productId)) {
            throw new RuntimeException("Rețetă circulară detectată pentru produsul ID: " + productId);
        }

        processingPath.add(productId);
        try {
            Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            BigDecimal adjustedInputQuantity = adjustForTechnologicalSheet(product, quantity);

            List<RecipeItem> recipeItems = recipeItemRepository.findByProductWithIngredient(product);
            if (recipeItems.isEmpty()) {
                throw new RuntimeException("No recipe defined for product: " + product.getName());
            }

            // 1) Handle semifinished product components (produce deficit recursively, then consume)
            for (RecipeItem item : recipeItems) {
                if (item.getComponentType() == RecipeItem.ComponentType.PRODUCT && item.getSourceProduct() != null) {
                    Product sourceProduct = item.getSourceProduct();
                    BigDecimal needed = item.getTotalRequiredQuantity(adjustedInputQuantity);
                    BigDecimal available = sourceProduct.getPhysicalStock() != null ? sourceProduct.getPhysicalStock() : BigDecimal.ZERO;

                    if (available.compareTo(needed) < 0) {
                        BigDecimal deficit = needed.subtract(available);
                        executeProductionInternal(sourceProduct.getId(), deficit, processingPath);
                    }
                    productService.removeStock(sourceProduct.getId(), needed);
                }
            }

            // 2) Aggregate and validate raw ingredient requirements
            Map<Long, BigDecimal> requiredIngredients = new HashMap<>();
            for (RecipeItem item : recipeItems) {
                if (item.getComponentType() == RecipeItem.ComponentType.INGREDIENT && item.getIngredient() != null) {
                    BigDecimal required = toIngredientUnit(item, item.getTotalRequiredQuantity(adjustedInputQuantity));
                    requiredIngredients.merge(item.getIngredient().getId(), required, BigDecimal::add);
                }
            }

            for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
                if (!ingredientService.hasSufficientStock(entry.getKey(), entry.getValue())) {
                    Ingredient ingredient = ingredientService.getIngredientById(entry.getKey()).orElse(null);
                    throw new RuntimeException("Insufficient stock for ingredient: " + (ingredient != null ? ingredient.getName() : entry.getKey()) +
                        ". Required: " + entry.getValue());
                }
            }

            // 3) Consume ingredients FEFO
            for (Map.Entry<Long, BigDecimal> entry : requiredIngredients.entrySet()) {
                Ingredient ingredient = ingredientService.getIngredientById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Ingredient not found: " + entry.getKey()));
                ingredientService.removeStock(entry.getKey(), entry.getValue());
                stockService.consumeFefo(
                    ingredient,
                    entry.getValue(),
                    ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : null,
                    "PRODUCTION",
                    productId
                );
            }

            // 4) Increase finished product stock
            productService.addStock(productId, quantity);

            // 5) Save production report
            ProductionReport report = new ProductionReport();
            report.setProduct(product);
            report.setQuantityProduced(quantity);
            report.setProductionDate(LocalDateTime.now());
            report.setStatus(ProductionReport.ProductionStatus.COMPLETED);
            productionReportRepository.save(report);
        } finally {
            processingPath.remove(productId);
        }
    }

    private void collectIngredientRequirements(
        Product product,
        BigDecimal targetOutputQuantity,
        Set<Long> processingPath,
        Map<Long, BigDecimal> ingredientRequirements,
        Map<Long, BigDecimal> virtualProductStocks
    ) {
        if (product == null || targetOutputQuantity == null || targetOutputQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (processingPath.contains(product.getId())) {
            throw new RuntimeException("Rețetă circulară detectată pentru produsul: " + product.getName());
        }

        processingPath.add(product.getId());
        try {
            BigDecimal adjustedInputQuantity = adjustForTechnologicalSheet(product, targetOutputQuantity);
            List<RecipeItem> recipeItems = recipeItemRepository.findByProductWithIngredient(product);
            if (recipeItems.isEmpty()) {
                throw new RuntimeException("No recipe defined for product: " + product.getName());
            }

            for (RecipeItem item : recipeItems) {
                if (item.getComponentType() == RecipeItem.ComponentType.INGREDIENT && item.getIngredient() != null) {
                    BigDecimal required = toIngredientUnit(item, item.getTotalRequiredQuantity(adjustedInputQuantity));
                    ingredientRequirements.merge(item.getIngredient().getId(), required, BigDecimal::add);
                    continue;
                }

                if (item.getComponentType() == RecipeItem.ComponentType.PRODUCT && item.getSourceProduct() != null) {
                    Product sourceProduct = item.getSourceProduct();
                    BigDecimal needed = item.getTotalRequiredQuantity(adjustedInputQuantity);

                    BigDecimal remaining = virtualProductStocks.get(sourceProduct.getId());
                    if (remaining == null) {
                        remaining = sourceProduct.getPhysicalStock() != null ? sourceProduct.getPhysicalStock() : BigDecimal.ZERO;
                    }

                    if (remaining.compareTo(needed) >= 0) {
                        virtualProductStocks.put(sourceProduct.getId(), remaining.subtract(needed));
                    } else {
                        BigDecimal deficit = needed.subtract(remaining.max(BigDecimal.ZERO));
                        virtualProductStocks.put(sourceProduct.getId(), BigDecimal.ZERO);
                        collectIngredientRequirements(sourceProduct, deficit, processingPath, ingredientRequirements, virtualProductStocks);
                    }
                }
            }
        } finally {
            processingPath.remove(product.getId());
        }
    }
}
