package com.bakerymanager.smartbill.inventory.domain;

import com.bakerymanager.entity.Ingredient;

import java.util.List;

public interface InventoryPort {

    List<Ingredient> getAllIngredients();

    Ingredient saveIngredient(Ingredient ingredient);

    void deleteIngredient(Long ingredientId);
}
