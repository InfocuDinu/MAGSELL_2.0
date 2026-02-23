package com.bakerymanager.smartbill.reports.api;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;

import java.util.List;

public interface ReportingFacade {

    List<Product> getAvailableProducts();

    List<Ingredient> getAllIngredients();

    List<Ingredient> getLowStockIngredients();
}
