package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recipe_items")
public class RecipeItem {

    public enum ComponentType {
        INGREDIENT,
        PRODUCT
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_product_id")
    private Product sourceProduct;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", length = 20, nullable = false)
    private ComponentType componentType = ComponentType.INGREDIENT;
    
    @Column(name = "required_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal requiredQuantity;

    @Column(name = "unit", length = 20)
    private String unit;
    
    @PrePersist
    protected void onCreate() {
        if (requiredQuantity == null) {
            requiredQuantity = BigDecimal.ZERO;
        }
        if (componentType == null) {
            componentType = ComponentType.INGREDIENT;
        }
    }
    
    public BigDecimal getTotalRequiredQuantity(BigDecimal productQuantity) {
        return requiredQuantity.multiply(productQuantity);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public Product getSourceProduct() { return sourceProduct; }
    public void setSourceProduct(Product sourceProduct) { this.sourceProduct = sourceProduct; }

    public ComponentType getComponentType() { return componentType; }
    public void setComponentType(ComponentType componentType) { this.componentType = componentType; }
    
    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(BigDecimal requiredQuantity) { this.requiredQuantity = requiredQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public Long getIngredientId() { 
        return ingredient != null ? ingredient.getId() : null; 
    }

    public Long getSourceProductId() {
        return sourceProduct != null ? sourceProduct.getId() : null;
    }
}
