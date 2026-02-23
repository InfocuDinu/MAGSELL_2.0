package com.bakerymanager.smartbill.reports.infrastructure;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.ProductService;
import com.bakerymanager.smartbill.reports.domain.ReportingPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportingPortAdapter implements ReportingPort {

    private final ProductService productService;
    private final IngredientService ingredientService;

    public ReportingPortAdapter(ProductService productService, IngredientService ingredientService) {
        this.productService = productService;
        this.ingredientService = ingredientService;
    }

    @Override
    public List<Product> getAvailableProducts() {
        return productService.getAvailableProducts();
    }

    @Override
    public List<Ingredient> getAllIngredients() {
        return ingredientService.getAllIngredients();
    }

    @Override
    public List<Ingredient> getLowStockIngredients() {
        return ingredientService.getLowStockIngredients();
    }
}
