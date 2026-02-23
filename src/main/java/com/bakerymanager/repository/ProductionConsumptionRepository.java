package com.bakerymanager.repository;

import com.bakerymanager.entity.ProductionConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductionConsumptionRepository extends JpaRepository<ProductionConsumption, Long> {

	List<ProductionConsumption> findByProductionOrderLineId(Long productionOrderLineId);

	@Query("SELECT pc FROM ProductionConsumption pc " +
		   "JOIN pc.productionOrderLine pol " +
		   "JOIN pol.productionOrder po " +
		   "WHERE po.plannedDate BETWEEN :startDate AND :endDate")
	List<ProductionConsumption> findByPlannedDateBetween(@Param("startDate") LocalDate startDate,
													   @Param("endDate") LocalDate endDate);
}
