package com.bakerymanager.service;

import com.bakerymanager.entity.Customer;
import com.bakerymanager.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    // Create or update customer
    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
    
    // Find customer by ID
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }
    
    // Find customer by phone
    public Optional<Customer> getCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }
    
    // Find customer by email
    public Optional<Customer> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }
    
    // Search customers by name
    public List<Customer> searchCustomersByName(String name) {
        return customerRepository.findByNameContainingIgnoreCase(name);
    }
    
    // Get all active customers
    public List<Customer> getAllActiveCustomers() {
        return customerRepository.findByIsActiveTrueOrderByNameAsc();
    }
    
    // Get all customers
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    
    // Get top customers by total purchases
    public List<Customer> getTopCustomers() {
        return customerRepository.findTopCustomersByPurchases();
    }
    
    // Get customers with high loyalty points
    public List<Customer> getHighLoyaltyCustomers(Integer minPoints) {
        return customerRepository.findByLoyaltyPointsGreaterThanEqual(minPoints);
    }
    
    // Add loyalty points to customer
    public void addLoyaltyPoints(Long customerId, int points) {
        Optional<Customer> optCustomer = customerRepository.findById(customerId);
        if (optCustomer.isPresent()) {
            Customer customer = optCustomer.get();
            customer.addLoyaltyPoints(points);
            customerRepository.save(customer);
        }
    }
    
    // Redeem loyalty points
    public boolean redeemLoyaltyPoints(Long customerId, int points) {
        Optional<Customer> optCustomer = customerRepository.findById(customerId);
        if (optCustomer.isPresent()) {
            Customer customer = optCustomer.get();
            if (customer.getLoyaltyPoints() >= points) {
                customer.redeemLoyaltyPoints(points);
                customerRepository.save(customer);
                return true;
            }
        }
        return false;
    }
    
    // Update total purchases (called from SaleService)
    public void updateTotalPurchases(Long customerId, BigDecimal amount) {
        Optional<Customer> optCustomer = customerRepository.findById(customerId);
        if (optCustomer.isPresent()) {
            Customer customer = optCustomer.get();
            customer.updateTotalPurchases(amount);
            
            // Award loyalty points: 1 point per 10 LEI spent
            int pointsToAdd = amount.divide(BigDecimal.TEN).intValue();
            if (pointsToAdd > 0) {
                customer.addLoyaltyPoints(pointsToAdd);
            }
            
            customerRepository.save(customer);
        }
    }
    
    // Deactivate customer
    public void deactivateCustomer(Long customerId) {
        Optional<Customer> optCustomer = customerRepository.findById(customerId);
        if (optCustomer.isPresent()) {
            Customer customer = optCustomer.get();
            customer.setIsActive(false);
            customerRepository.save(customer);
        }
    }
    
    // Reactivate customer
    public void reactivateCustomer(Long customerId) {
        Optional<Customer> optCustomer = customerRepository.findById(customerId);
        if (optCustomer.isPresent()) {
            Customer customer = optCustomer.get();
            customer.setIsActive(true);
            customerRepository.save(customer);
        }
    }
    
    // Delete customer
    public void deleteCustomer(Long customerId) {
        customerRepository.deleteById(customerId);
    }
    
    // Check if phone exists
    public boolean phoneExists(String phone) {
        return customerRepository.existsByPhone(phone);
    }
    
    // Check if email exists
    public boolean emailExists(String email) {
        return customerRepository.existsByEmail(email);
    }
}
