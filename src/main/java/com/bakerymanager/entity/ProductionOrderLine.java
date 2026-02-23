package com.bakerymanager.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "production_order_lines")
public class ProductionOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "planned_quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal plannedQuantity = BigDecimal.ZERO;

    @Column(name = "actual_quantity", precision = 10, scale = 3)
    private BigDecimal actualQuantity = BigDecimal.ZERO;

    @Column(name = "unit", length = 20)
    private String unit;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProductionOrder getProductionOrder() { return productionOrder; }
    public void setProductionOrder(ProductionOrder productionOrder) { this.productionOrder = productionOrder; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getPlannedQuantity() { return plannedQuantity; }
    public void setPlannedQuantity(BigDecimal plannedQuantity) { this.plannedQuantity = plannedQuantity; }

    public BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(BigDecimal actualQuantity) { this.actualQuantity = actualQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
