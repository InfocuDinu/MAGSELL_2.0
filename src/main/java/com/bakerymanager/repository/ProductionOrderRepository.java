package com.bakerymanager.repository;

import com.bakerymanager.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    List<ProductionOrder> findByPlannedDateBetweenOrderByPlannedDate(LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT po FROM ProductionOrder po " +
           "LEFT JOIN FETCH po.lines l " +
           "LEFT JOIN FETCH l.product " +
           "WHERE po.id = :id")
    Optional<ProductionOrder> findByIdWithLines(@Param("id") Long id);
}
