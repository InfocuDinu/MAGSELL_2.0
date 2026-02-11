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
    
    @Query("SELECT pr FROM ProductionReport pr JOIN FETCH pr.product WHERE pr.product = :product ORDER BY pr.productionDate DESC")
    List<ProductionReport> findByProductOrderByProductionDateDesc(@Param("product") Product product);
    
    @Query("SELECT pr FROM ProductionReport pr JOIN FETCH pr.product WHERE pr.status = :status ORDER BY pr.productionDate DESC")
    List<ProductionReport> findByStatusOrderByProductionDateDesc(@Param("status") ProductionReport.ProductionStatus status);
    
    @Query("SELECT pr FROM ProductionReport pr JOIN FETCH pr.product WHERE pr.productionDate BETWEEN :startDate AND :endDate ORDER BY pr.productionDate DESC")
    List<ProductionReport> findByProductionDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pr FROM ProductionReport pr JOIN FETCH pr.product ORDER BY pr.productionDate DESC")
    List<ProductionReport> findAllOrderByProductionDateDesc();
}
