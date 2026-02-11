package com.bakerymanager.repository;

import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductionReportRepository extends JpaRepository<ProductionReport, Long> {
    
    List<ProductionReport> findByProductOrderByProductionDateDesc(Product product);
    
    List<ProductionReport> findByStatusOrderByProductionDateDesc(ProductionReport.ProductionStatus status);
    
    @Query("SELECT pr FROM ProductionReport pr WHERE pr.productionDate BETWEEN :startDate AND :endDate ORDER BY pr.productionDate DESC")
    List<ProductionReport> findByProductionDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pr FROM ProductionReport pr ORDER BY pr.productionDate DESC")
    List<ProductionReport> findAllOrderByProductionDateDesc();
}
