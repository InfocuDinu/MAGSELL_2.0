package com.bakerymanager.smartbill.inventory.application;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.smartbill.inventory.api.InventoryFacade;
import com.bakerymanager.smartbill.inventory.domain.InventoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryFacadeImpl implements InventoryFacade {

    private final InventoryPort inventoryPort;

    public InventoryFacadeImpl(InventoryPort inventoryPort) {
        this.inventoryPort = inventoryPort;
    }

    @Override
    public List<Ingredient> getAllIngredients() {
        return inventoryPort.getAllIngredients();
    }

    @Override
    public Ingredient saveIngredient(Ingredient ingredient) {
        return inventoryPort.saveIngredient(ingredient);
    }

    @Override
    public void deleteIngredient(Long ingredientId) {
        inventoryPort.deleteIngredient(ingredientId);
    }
}
