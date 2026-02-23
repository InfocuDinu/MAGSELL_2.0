package com.bakerymanager.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "production_consumptions")
public class ProductionConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_line_id", nullable = false)
    private ProductionOrderLine productionOrderLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "standard_quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal standardQuantity = BigDecimal.ZERO;

    @Column(name = "actual_quantity", precision = 10, scale = 3)
    private BigDecimal actualQuantity = BigDecimal.ZERO;

    @Column(name = "unit", length = 20)
    private String unit;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProductionOrderLine getProductionOrderLine() { return productionOrderLine; }
    public void setProductionOrderLine(ProductionOrderLine productionOrderLine) { this.productionOrderLine = productionOrderLine; }

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public BigDecimal getStandardQuantity() { return standardQuantity; }
    public void setStandardQuantity(BigDecimal standardQuantity) { this.standardQuantity = standardQuantity; }

    public BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(BigDecimal actualQuantity) { this.actualQuantity = actualQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
