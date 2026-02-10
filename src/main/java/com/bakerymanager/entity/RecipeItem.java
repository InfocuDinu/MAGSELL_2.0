package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recipe_items")
public class RecipeItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;
    
    @Column(name = "required_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal requiredQuantity;
    
    @PrePersist
    protected void onCreate() {
        if (requiredQuantity == null) {
            requiredQuantity = BigDecimal.ZERO;
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
    
    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(BigDecimal requiredQuantity) { this.requiredQuantity = requiredQuantity; }
    
    public Long getIngredientId() { 
        return ingredient != null ? ingredient.getId() : null; 
    }
}
