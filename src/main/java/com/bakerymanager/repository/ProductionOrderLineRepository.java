package com.bakerymanager.repository;

import com.bakerymanager.entity.ProductionOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionOrderLineRepository extends JpaRepository<ProductionOrderLine, Long> {
}
