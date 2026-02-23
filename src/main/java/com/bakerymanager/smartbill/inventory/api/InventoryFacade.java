package com.bakerymanager.smartbill.inventory.api;

import com.bakerymanager.entity.Ingredient;

import java.util.List;

public interface InventoryFacade {

    List<Ingredient> getAllIngredients();

    Ingredient saveIngredient(Ingredient ingredient);

    void deleteIngredient(Long ingredientId);
}
