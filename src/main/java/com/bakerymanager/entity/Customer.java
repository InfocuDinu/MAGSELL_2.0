package com.bakerymanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 100)
    private String email;
    
    @Column(name = "loyalty_points")
    private Integer loyaltyPoints = 0;
    
    @Column(name = "total_purchases", precision = 10, scale = 2)
    private java.math.BigDecimal totalPurchases = java.math.BigDecimal.ZERO;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "registration_date")
    private LocalDateTime registrationDate;
    
    @Column(name = "last_purchase_date")
    private LocalDateTime lastPurchaseDate;
    
    @Column(length = 500)
    private String notes;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Sale> sales;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomOrder> customOrders;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Customer() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.registrationDate = LocalDateTime.now();
        this.loyaltyPoints = 0;
        this.isActive = true;
    }
    
    public Customer(String name, String phone, String email) {
        this();
        this.name = name;
        this.phone = phone;
        this.email = email;
    }
    
    // Business methods
    public void addLoyaltyPoints(int points) {
        this.loyaltyPoints += points;
    }
    
    public void redeemLoyaltyPoints(int points) {
        if (this.loyaltyPoints >= points) {
            this.loyaltyPoints -= points;
        }
    }
    
    public void updateTotalPurchases(java.math.BigDecimal amount) {
        this.totalPurchases = this.totalPurchases.add(amount);
        this.lastPurchaseDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Integer getLoyaltyPoints() {
        return loyaltyPoints;
    }
    
    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
        this.updatedAt = LocalDateTime.now();
    }
    
    public java.math.BigDecimal getTotalPurchases() {
        return totalPurchases;
    }
    
    public void setTotalPurchases(java.math.BigDecimal totalPurchases) {
        this.totalPurchases = totalPurchases;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }
    
    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }
    
    public LocalDateTime getLastPurchaseDate() {
        return lastPurchaseDate;
    }
    
    public void setLastPurchaseDate(LocalDateTime lastPurchaseDate) {
        this.lastPurchaseDate = lastPurchaseDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }
    
    public List<Sale> getSales() {
        return sales;
    }
    
    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }
    
    public List<CustomOrder> getCustomOrders() {
        return customOrders;
    }
    
    public void setCustomOrders(List<CustomOrder> customOrders) {
        this.customOrders = customOrders;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
