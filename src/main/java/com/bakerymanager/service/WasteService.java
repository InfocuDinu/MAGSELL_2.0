package com.bakerymanager.service;

import com.bakerymanager.entity.Waste;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.repository.WasteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WasteService {
    
    @Autowired
    private WasteRepository wasteRepository;
    
    // Record waste
    public Waste recordWaste(Waste waste) {
        return wasteRepository.save(waste);
    }
    
    // Record product waste
    public Waste recordProductWaste(Product product, BigDecimal quantity, Waste.WasteReason reason, String recordedBy, String notes) {
        Waste waste = new Waste(product, quantity, reason);
        waste.setRecordedBy(recordedBy);
        waste.setNotes(notes);
        return wasteRepository.save(waste);
    }
    
    // Record ingredient waste
    public Waste recordIngredientWaste(Ingredient ingredient, BigDecimal quantity, Waste.WasteReason reason, String recordedBy, String notes) {
        Waste waste = new Waste(ingredient, quantity, reason);
        waste.setRecordedBy(recordedBy);
        waste.setNotes(notes);
        return wasteRepository.save(waste);
    }
    
    // Get waste by ID
    public Optional<Waste> getWasteById(Long id) {
        return wasteRepository.findById(id);
    }
    
    // Get all waste records
    public List<Waste> getAllWaste() {
        return wasteRepository.findAllByOrderByWasteDateDesc();
    }
    
    // Get waste by date range
    public List<Waste> getWasteByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return wasteRepository.findByWasteDateBetweenOrderByWasteDateDesc(startDate, endDate);
    }
    
    // Get waste today
    public List<Waste> getWasteToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return wasteRepository.findByWasteDateBetweenOrderByWasteDateDesc(startOfDay, endOfDay);
    }
    
    // Get waste this week
    public List<Waste> getWasteThisWeek() {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();
        return wasteRepository.findByWasteDateBetweenOrderByWasteDateDesc(startOfWeek, now);
    }
    
    // Get waste this month
    public List<Waste> getWasteThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();
        return wasteRepository.findByWasteDateBetweenOrderByWasteDateDesc(startOfMonth, now);
    }
    
    // Get waste by item type
    public List<Waste> getWasteByItemType(Waste.ItemType itemType) {
        return wasteRepository.findByItemTypeOrderByWasteDateDesc(itemType);
    }
    
    // Get waste by reason
    public List<Waste> getWasteByReason(Waste.WasteReason reason) {
        return wasteRepository.findByReasonOrderByWasteDateDesc(reason);
    }
    
    // Get waste by product
    public List<Waste> getWasteByProduct(Product product) {
        return wasteRepository.findByProductOrderByWasteDateDesc(product);
    }
    
    // Get waste by ingredient
    public List<Waste> getWasteByIngredient(Ingredient ingredient) {
        return wasteRepository.findByIngredientOrderByWasteDateDesc(ingredient);
    }
    
    // Calculate total waste cost
    public BigDecimal getTotalWasteCost(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal total = wasteRepository.getTotalWasteCost(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    // Calculate total waste cost today
    public BigDecimal getWasteCostToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return getTotalWasteCost(startOfDay, endOfDay);
    }
    
    // Calculate total waste cost this month
    public BigDecimal getWasteCostThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();
        return getTotalWasteCost(startOfMonth, now);
    }
    
    // Get waste by reason in date range
    public List<Waste> getWasteByReasonAndDateRange(Waste.WasteReason reason, LocalDateTime startDate, LocalDateTime endDate) {
        return wasteRepository.findByReasonAndDateRange(reason, startDate, endDate);
    }
    
    // Delete waste record
    public void deleteWaste(Long wasteId) {
        wasteRepository.deleteById(wasteId);
    }
}
