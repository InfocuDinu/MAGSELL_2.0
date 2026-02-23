package com.bakerymanager.smartbill.inventory.infrastructure;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.smartbill.inventory.domain.InventoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryPortAdapter implements InventoryPort {

    private final IngredientService ingredientService;

    public InventoryPortAdapter(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @Override
    public List<Ingredient> getAllIngredients() {
        return ingredientService.getAllIngredients();
    }

    @Override
    public Ingredient saveIngredient(Ingredient ingredient) {
        return ingredientService.saveIngredient(ingredient);
    }

    @Override
    public void deleteIngredient(Long ingredientId) {
        ingredientService.deleteIngredient(ingredientId);
    }
}
