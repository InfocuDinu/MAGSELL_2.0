package com.bakerymanager.repository;

import com.bakerymanager.entity.CustomOrder;
import com.bakerymanager.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomOrderRepository extends JpaRepository<CustomOrder, Long> {
    
    // Find by customer
    @Query("SELECT co FROM CustomOrder co JOIN FETCH co.customer WHERE co.customer = :customer ORDER BY co.dueDate ASC")
    List<CustomOrder> findByCustomerOrderByDueDateAsc(@Param("customer") Customer customer);
    
    // Find by status
    @Query("SELECT co FROM CustomOrder co JOIN FETCH co.customer WHERE co.status = :status ORDER BY co.dueDate ASC")
    List<CustomOrder> findByStatusOrderByDueDateAsc(@Param("status") CustomOrder.OrderStatus status);
    
    // Find pending orders
    @Query("SELECT co FROM CustomOrder co JOIN FETCH co.customer WHERE co.status = 'PENDING' OR co.status = 'CONFIRMED' OR co.status = 'IN_PROGRESS' ORDER BY co.dueDate ASC")
    List<CustomOrder> findPendingOrders();
    
    // Find overdue orders
    @Query("SELECT co FROM CustomOrder co JOIN FETCH co.customer WHERE co.dueDate < :now AND co.status != 'DELIVERED' AND co.status != 'CANCELLED' ORDER BY co.dueDate ASC")
    List<CustomOrder> findOverdueOrders(@Param("now") LocalDateTime now);
    
    // Find orders due between dates
    @Query("SELECT co FROM CustomOrder co JOIN FETCH co.customer WHERE co.dueDate BETWEEN :startDate AND :endDate ORDER BY co.dueDate ASC")
    List<CustomOrder> findByDueDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find all orders ordered by due date
    @Query("SELECT co FROM CustomOrder co JOIN FETCH co.customer ORDER BY co.dueDate ASC")
    List<CustomOrder> findAllOrderByDueDateAsc();
    
    // Find orders ready for pickup
    @Query("SELECT co FROM CustomOrder co JOIN FETCH co.customer WHERE co.status = 'READY' ORDER BY co.completionDate ASC")
    List<CustomOrder> findReadyOrders();
}
