package com.bakerymanager.service;

import com.bakerymanager.entity.CustomOrder;
import com.bakerymanager.entity.Customer;
import com.bakerymanager.repository.CustomOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomOrderService {
    
    @Autowired
    private CustomOrderRepository customOrderRepository;
    
    // Create or update custom order
    public CustomOrder saveCustomOrder(CustomOrder customOrder) {
        return customOrderRepository.save(customOrder);
    }
    
    // Find order by ID
    public Optional<CustomOrder> getCustomOrderById(Long id) {
        return customOrderRepository.findById(id);
    }
    
    // Get all orders
    public List<CustomOrder> getAllOrders() {
        return customOrderRepository.findAllOrderByDueDateAsc();
    }
    
    // Get orders by customer
    public List<CustomOrder> getOrdersByCustomer(Customer customer) {
        return customOrderRepository.findByCustomerOrderByDueDateAsc(customer);
    }
    
    // Get orders by status
    public List<CustomOrder> getOrdersByStatus(CustomOrder.OrderStatus status) {
        return customOrderRepository.findByStatusOrderByDueDateAsc(status);
    }
    
    // Get pending orders (PENDING, CONFIRMED, IN_PROGRESS)
    public List<CustomOrder> getPendingOrders() {
        return customOrderRepository.findPendingOrders();
    }
    
    // Get overdue orders
    public List<CustomOrder> getOverdueOrders() {
        return customOrderRepository.findOverdueOrders(LocalDateTime.now());
    }
    
    // Get orders ready for pickup
    public List<CustomOrder> getReadyOrders() {
        return customOrderRepository.findReadyOrders();
    }
    
    // Get orders due between dates
    public List<CustomOrder> getOrdersDueBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return customOrderRepository.findByDueDateBetween(startDate, endDate);
    }
    
    // Get orders due today
    public List<CustomOrder> getOrdersDueToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return customOrderRepository.findByDueDateBetween(startOfDay, endOfDay);
    }
    
    // Get orders due this week
    public List<CustomOrder> getOrdersDueThisWeek() {
        LocalDateTime startOfWeek = LocalDateTime.now();
        LocalDateTime endOfWeek = LocalDateTime.now().plusDays(7);
        return customOrderRepository.findByDueDateBetween(startOfWeek, endOfWeek);
    }
    
    // Update order status
    public void updateOrderStatus(Long orderId, CustomOrder.OrderStatus newStatus) {
        Optional<CustomOrder> optOrder = customOrderRepository.findById(orderId);
        if (optOrder.isPresent()) {
            CustomOrder order = optOrder.get();
            order.updateStatus(newStatus);
            customOrderRepository.save(order);
        }
    }
    
    // Add advance payment
    public void addAdvancePayment(Long orderId, BigDecimal amount) {
        Optional<CustomOrder> optOrder = customOrderRepository.findById(orderId);
        if (optOrder.isPresent()) {
            CustomOrder order = optOrder.get();
            order.addAdvancePayment(amount);
            customOrderRepository.save(order);
        }
    }
    
    // Mark order as ready
    public void markAsReady(Long orderId) {
        updateOrderStatus(orderId, CustomOrder.OrderStatus.READY);
    }
    
    // Mark order as delivered
    public void markAsDelivered(Long orderId) {
        updateOrderStatus(orderId, CustomOrder.OrderStatus.DELIVERED);
    }
    
    // Cancel order
    public void cancelOrder(Long orderId) {
        updateOrderStatus(orderId, CustomOrder.OrderStatus.CANCELLED);
    }
    
    // Delete order
    public void deleteOrder(Long orderId) {
        customOrderRepository.deleteById(orderId);
    }
    
    // Get orders count by status
    public long getOrdersCountByStatus(CustomOrder.OrderStatus status) {
        return getOrdersByStatus(status).size();
    }
    
    // Get total revenue from custom orders
    public BigDecimal getTotalRevenue() {
        List<CustomOrder> allOrders = getAllOrders();
        return allOrders.stream()
            .filter(order -> order.getStatus() == CustomOrder.OrderStatus.DELIVERED)
            .map(CustomOrder::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Get pending revenue (not yet delivered)
    public BigDecimal getPendingRevenue() {
        List<CustomOrder> pendingOrders = getPendingOrders();
        return pendingOrders.stream()
            .map(CustomOrder::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
