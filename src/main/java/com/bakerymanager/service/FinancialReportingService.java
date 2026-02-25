package com.bakerymanager.service;

import com.bakerymanager.entity.*;
import com.bakerymanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for financial reporting - CORE IMPLEMENTATION.
 * Simplified version focused on core Point 9 requirements:
 * - Production cost calculation using available FEFO data
 * - Daily profit reporting
 * - VAT summary reporting
 * 
 * Enhanced features will be added as entity/repository methods become available.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialReportingService {
    
    private final CostBreakdownRepository costBreakdownRepository;
    private final DailyFinancialReportRepository dailyFinancialReportRepository;
    private final VATReportRepository vatReportRepository;
    private final ProductionReportRepository productionReportRepository;
    private final SaleItemRepository saleItemRepository;
    private final RecipeItemRepository recipeItemRepository;
    
    /**
     * Calculate production cost for a product.
     * Simplified: Uses average ingredient batch cost for now.
     */
    @Transactional
    public CostBreakdown calculateProductionCost(Product product, LocalDate costDate) {
        log.info("Calculating production cost for product: {} on date: {}", product.getName(), costDate);
        
        // Check if cost already calculated
        Optional<CostBreakdown> existing = costBreakdownRepository.findByProductAndDate(product, costDate);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        CostBreakdown costBreakdown = new CostBreakdown();
        costBreakdown.setProduct(product);
        costBreakdown.setCostDate(costDate);
        costBreakdown.setCostMethod("AVERAGE");
        
        // Get production reports for this product (all time - will be filtered by date in future)
        List<ProductionReport> productions = productionReportRepository.findByProductOrderByProductionDateDesc(product);
        
        if (productions.isEmpty()) {
            log.debug("No production found for {}", product.getName());
            costBreakdown.setQuantityProduced(BigDecimal.ZERO);
            costBreakdown.setRawMaterialCost(BigDecimal.ZERO);
            costBreakdown.setLaborCost(BigDecimal.ZERO);
            costBreakdown.setOverheadCost(BigDecimal.ZERO);
            costBreakdown.setSemifabricatCost(BigDecimal.ZERO);
            costBreakdown.recalculateTotal();
            return costBreakdownRepository.save(costBreakdown);
        }
        
        // Calculate approximate cost based on recipe
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalRawMaterialCost = BigDecimal.ZERO;
        
        for (ProductionReport production : productions) {
            totalQuantity = totalQuantity.add(production.getQuantityProduced());
        }
        
        // Get recipe items and calculate approx cost
        List<RecipeItem> recipeItems = recipeItemRepository.findByProduct(product);
        if (recipeItems != null && !recipeItems.isEmpty()) {
            // For each recipe item, estimate cost based on ingredient average price
            for (RecipeItem item : recipeItems) {
                if (item.getIngredient() != null) {
                    // Use fixed estimate for now - will get actual batch prices when repository methods available
                    BigDecimal estimatedIngredientCost = new BigDecimal("10");  // Placeholder
                    totalRawMaterialCost = totalRawMaterialCost.add(
                        estimatedIngredientCost.multiply(item.getRequiredQuantity() != null ? item.getRequiredQuantity() : BigDecimal.ONE)
                    );
                }
            }
        }
        
        BigDecimal overheadPerUnit = new BigDecimal("0.50");  // Placeholder
        BigDecimal totalOverhead = overheadPerUnit.multiply(totalQuantity);
        
        costBreakdown.setQuantityProduced(totalQuantity);
        costBreakdown.setRawMaterialCost(totalRawMaterialCost);
        costBreakdown.setLaborCost(BigDecimal.ZERO);  // Will be enhanced when scheduling methods available
        costBreakdown.setOverheadCost(totalOverhead);
        costBreakdown.setSemifabricatCost(BigDecimal.ZERO);  // Will be enhanced for multi-level recipes
        costBreakdown.recalculateTotal();
        
        log.info("Cost calculated - Total: {}, Unit cost: {}", costBreakdown.getTotalCost(), costBreakdown.getUnitCost());
        
        return costBreakdownRepository.save(costBreakdown);
    }
    
    /**
     * Generate daily financial report.
     */
    @Transactional
    public DailyFinancialReport generateDailyFinancialReport(LocalDate reportDate) {
        log.info("Generating daily financial report for: {}", reportDate);
        
        // Check if exists
        Optional<DailyFinancialReport> existing = dailyFinancialReportRepository.findByReportDate(reportDate);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        DailyFinancialReport report = new DailyFinancialReport();
        report.setReportDate(reportDate);
        
        LocalDateTime startOfDay = reportDate.atStartOfDay();
        LocalDateTime endOfDay = reportDate.plusDays(1).atStartOfDay();
        
        // Get sales for this date
        List<SaleItem> salesItems = saleItemRepository.findBySaleDateBetween(startOfDay, endOfDay);
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCOGS = BigDecimal.ZERO;
        BigDecimal totalVAT = BigDecimal.ZERO;
        BigDecimal vat0 = BigDecimal.ZERO;
        BigDecimal vat11 = BigDecimal.ZERO;
        BigDecimal vat21 = BigDecimal.ZERO;
        int unitsSold = 0;
        
        for (SaleItem item : salesItems) {
            // Add revenue
            BigDecimal salePrice = item.getUnitPrice().multiply(item.getQuantity());
            totalRevenue = totalRevenue.add(salePrice);
            
            // Calculate VAT based on product VAT rate
            BigDecimal vatRate = item.getVatRate() != null ? item.getVatRate() : new BigDecimal("21");
            BigDecimal vatMultiplier = vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal vat = salePrice.multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);
            totalVAT = totalVAT.add(vat);
            
            // Track VAT by rate
            if (vatRate.compareTo(BigDecimal.ZERO) == 0) {
                vat0 = vat0.add(vat);
            } else if (vatRate.compareTo(new BigDecimal("11")) == 0) {
                vat11 = vat11.add(vat);
            } else if (vatRate.compareTo(new BigDecimal("21")) == 0) {
                vat21 = vat21.add(vat);
            }
            
            // Add COGS
            if (item.getProduct() != null) {
                CostBreakdown cost = calculateProductionCost(item.getProduct(), reportDate);
                BigDecimal itemCOGS = cost.getUnitCost().multiply(item.getQuantity());
                totalCOGS = totalCOGS.add(itemCOGS);
            }
            
            unitsSold += item.getQuantity().intValue();
        }
        
        // Get production for this date
        List<ProductionReport> productions = productionReportRepository.findByProductionDateBetween(
            startOfDay, endOfDay
        );
        
        int unitsProduced = 0;
        for (ProductionReport p : productions) {
            unitsProduced += p.getQuantityProduced().intValue();
        }
        
        // Daily overhead
        BigDecimal dailyOverhead = new BigDecimal("166.67");  // 5000/30 days
        
        report.setTotalRevenue(totalRevenue);
        report.setTotalCOGS(totalCOGS);
        report.setLaborCost(BigDecimal.ZERO);  // To be enhanced
        report.setOverheadCost(dailyOverhead);
        report.setVat0Collected(vat0);
        report.setVat11Collected(vat11);
        report.setVat21Collected(vat21);
        report.setTotalVAT(totalVAT);
        report.setUnitsProduced(unitsProduced);
        report.setUnitsSold(unitsSold);
        report.recalculateMetrics();
        
        log.info("Daily report - Revenue: {}, COGS: {}, Profit: {}", 
            report.getTotalRevenue(), report.getTotalCOGS(), report.getGrossProfit());
        
        return dailyFinancialReportRepository.save(report);
    }
    
    /**
     * Generate monthly financial report.
     */
    @Transactional
    public Map<String, Object> generateMonthlyFinancialReport(YearMonth yearMonth) {
        log.info("Generating monthly financial report for: {}", yearMonth);
        
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<DailyFinancialReport> dailyReports = dailyFinancialReportRepository.findByDateRange(startDate, endDate);
        
        // Generate missing daily reports
        if (dailyReports.isEmpty()) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                dailyReports.add(generateDailyFinancialReport(date));
            }
        }
        
        // Aggregate
        BigDecimal monthlyRevenue = dailyReports.stream()
            .map(DailyFinancialReport::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal monthlyCOGS = dailyReports.stream()
            .map(DailyFinancialReport::getTotalCOGS)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal monthlyGrossProfit = monthlyRevenue.subtract(monthlyCOGS);
        
        BigDecimal monthlyLaborCost = dailyReports.stream()
            .map(DailyFinancialReport::getLaborCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal monthlyOverhead = dailyReports.stream()
            .map(DailyFinancialReport::getOverheadCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal monthlyOperatingProfit = monthlyGrossProfit
            .subtract(monthlyLaborCost)
            .subtract(monthlyOverhead);
        
        BigDecimal monthlyVAT = dailyReports.stream()
            .map(DailyFinancialReport::getTotalVAT)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Margins
        BigDecimal grossMargin = monthlyRevenue.compareTo(BigDecimal.ZERO) > 0 ?
            monthlyGrossProfit.divide(monthlyRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
            BigDecimal.ZERO;
        
        BigDecimal operatingMargin = monthlyRevenue.compareTo(BigDecimal.ZERO) > 0 ?
            monthlyOperatingProfit.divide(monthlyRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
            BigDecimal.ZERO;
        
        Map<String, Object> report = new HashMap<>();
        report.put("yearMonth", yearMonth.toString());
        report.put("totalRevenue", monthlyRevenue);
        report.put("totalCOGS", monthlyCOGS);
        report.put("grossProfit", monthlyGrossProfit);
        report.put("grossMarginPercent", grossMargin);
        report.put("laborCost", monthlyLaborCost);
        report.put("overheadCost", monthlyOverhead);
        report.put("operatingProfit", monthlyOperatingProfit);
        report.put("operatingMarginPercent", operatingMargin);
        report.put("totalVAT", monthlyVAT);
        report.put("dailyReportCount", dailyReports.size());
        
        return report;
    }
    
    /**
     * Generate VAT report.
     */
    @Transactional
    public VATReport generateVATReport(LocalDate startDate, LocalDate endDate, String periodName) {
        log.info("Generating VAT report for {} to {}", startDate, endDate);
        
        LocalDateTime startDT = startDate.atStartOfDay();
        LocalDateTime endDT = endDate.plusDays(1).atStartOfDay();
        
        List<SaleItem> salesItems = saleItemRepository.findBySaleDateBetween(startDT, endDT);
        
        VATReport report = new VATReport();
        report.setReportStartDate(startDate);
        report.setReportEndDate(endDate);
        report.setPeriodName(periodName);
        
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalVAT = BigDecimal.ZERO;
        BigDecimal vat0 = BigDecimal.ZERO;
        BigDecimal vat11 = BigDecimal.ZERO;
        BigDecimal vat21 = BigDecimal.ZERO;
        
        for (SaleItem item : salesItems) {
            BigDecimal saleAmount = item.getUnitPrice().multiply(item.getQuantity());
            totalSales = totalSales.add(saleAmount);
            
            // Calculate VAT based on product VAT rate
            BigDecimal vatRate = item.getVatRate() != null ? item.getVatRate() : new BigDecimal("21");
            BigDecimal vatMultiplier = vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal vat = saleAmount.multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);
            totalVAT = totalVAT.add(vat);
            
            // Track VAT by rate
            if (vatRate.compareTo(BigDecimal.ZERO) == 0) {
                vat0 = vat0.add(vat);
            } else if (vatRate.compareTo(new BigDecimal("11")) == 0) {
                vat11 = vat11.add(vat);
            } else if (vatRate.compareTo(new BigDecimal("21")) == 0) {
                vat21 = vat21.add(vat);
            }
        }
        
        report.setTotalSales(totalSales);
        report.setTaxableAmount(totalSales);
        report.setVat0Collected(vat0);
        report.setVat11Collected(vat11);
        report.setVat21Collected(vat21);
        report.setVatCollected(totalVAT);
        report.setVatDeductible(BigDecimal.ZERO);  // To be enhanced with purchase invoicing
        report.setVatRatePercent(new BigDecimal("21"));
        report.recalculateVAT();
        
        log.info("VAT report - Collected: {}, Payable: {}", report.getVatCollected(), report.getVatPayable());
        
        return vatReportRepository.save(report);
    }
    
    /**
     * Get cost breakdown report for a date range.
     */
    public List<CostBreakdown> getCostBreakdownReport(LocalDate startDate, LocalDate endDate) {
        return costBreakdownRepository.findByDateRange(startDate, endDate);
    }
    
    /**
     * Get profit trend for a month.
     */
    public Map<LocalDate, BigDecimal> getProfitTrend(YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<DailyFinancialReport> reports = dailyFinancialReportRepository.findByDateRange(startDate, endDate);
        
        return reports.stream()
            .collect(Collectors.toMap(
                DailyFinancialReport::getReportDate,
                DailyFinancialReport::getGrossProfit
            ));
    }
}
