package com.bakerymanager.smartbill.reports.domain;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;

import java.util.List;

public interface ReportingPort {

    List<Product> getAvailableProducts();

    List<Ingredient> getAllIngredients();

    List<Ingredient> getLowStockIngredients();
}
