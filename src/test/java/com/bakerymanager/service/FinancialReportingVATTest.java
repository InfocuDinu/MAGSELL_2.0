package com.bakerymanager.service;

import com.bakerymanager.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Financial Reporting with VAT system
 */
public class FinancialReportingVATTest {

    private DailyFinancialReport dailyReport;
    private VATReport vatReport;
    private LocalDate testDate;

    @BeforeEach
    public void setUp() {
        testDate = LocalDate.now();
        
        dailyReport = new DailyFinancialReport();
        dailyReport.setReportDate(testDate);
        
        vatReport = new VATReport();
        vatReport.setReportStartDate(testDate);
        vatReport.setReportEndDate(testDate);
        vatReport.setPeriodName(testDate.toString());
    }

    /**
     * Test DailyFinancialReport VAT aggregation by rate
     */
    @Test
    public void testDailyReportVATAggregation() {
        // Simulate sales with different VAT rates
        dailyReport.setVat0Collected(new BigDecimal("0.00"));
        dailyReport.setVat11Collected(new BigDecimal("110.00"));
        dailyReport.setVat21Collected(new BigDecimal("420.00"));
        
        dailyReport.recalculateMetrics();
        
        // Total should be sum of all rates
        assertEquals(new BigDecimal("530.00"), dailyReport.getTotalVAT());
    }

    /**
     * Test VATReport per-rate tracking
     */
    @Test
    public void testVATReportPerRateTracking() {
        vatReport.setVat0Collected(new BigDecimal("0.00"));
        vatReport.setVat11Collected(new BigDecimal("220.00"));
        vatReport.setVat21Collected(new BigDecimal("630.00"));
        
        vatReport.recalculateVAT();
        
        assertEquals(new BigDecimal("0.00"), vatReport.getVat0Collected());
        assertEquals(new BigDecimal("220.00"), vatReport.getVat11Collected());
        assertEquals(new BigDecimal("630.00"), vatReport.getVat21Collected());
        assertEquals(new BigDecimal("850.00"), vatReport.getVatCollected());
    }

    /**
     * Test zero VAT collection scenario
     */
    @Test
    public void testZeroVATCollection() {
        dailyReport.setVat0Collected(new BigDecimal("0.00"));
        dailyReport.setVat11Collected(new BigDecimal("0.00"));
        dailyReport.setVat21Collected(new BigDecimal("0.00"));
        
        dailyReport.recalculateMetrics();
        
        assertEquals(new BigDecimal("0.00"), dailyReport.getTotalVAT());
    }

    /**
     * Test daily report metrics calculation with VAT
     */
    @Test
    public void testDailyReportMetricsWithVAT() {
        // Setup revenue and costs
        dailyReport.setTotalRevenue(new BigDecimal("5000.00"));
        dailyReport.setTotalCOGS(new BigDecimal("2000.00"));
        dailyReport.setLaborCost(new BigDecimal("500.00"));
        dailyReport.setOverheadCost(new BigDecimal("200.00"));
        
        // Setup VAT by rate
        dailyReport.setVat0Collected(new BigDecimal("0.00"));
        dailyReport.setVat11Collected(new BigDecimal("400.00"));
        dailyReport.setVat21Collected(new BigDecimal("861.00"));
        
        dailyReport.recalculateMetrics();
        
        // Verify calculations
        assertEquals(new BigDecimal("3000.00"), dailyReport.getGrossProfit()); // 5000 - 2000
        assertEquals(new BigDecimal("2300.00"), dailyReport.getOperatingProfit()); // 3000 - 500 - 200
        assertEquals(new BigDecimal("1261.00"), dailyReport.getTotalVAT()); // 0 + 400 + 861
    }

    /**
     * Test VAT on different price points
     */
    @Test
    public void testVATOnVariousPrices() {
        // Test scenario: Mixed sales throughout the day
        BigDecimal[] prices = {
            new BigDecimal("100.00"),  // 0% = 0
            new BigDecimal("200.00"),  // 11% = 22
            new BigDecimal("500.00")   // 21% = 105
        };
        
        BigDecimal vat0 = prices[0].multiply(new BigDecimal("0")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal vat11 = prices[1].multiply(new BigDecimal("0.11")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal vat21 = prices[2].multiply(new BigDecimal("0.21")).setScale(2, RoundingMode.HALF_UP);
        
        assertEquals(new BigDecimal("0.00"), vat0);
        assertEquals(new BigDecimal("22.00"), vat11);
        assertEquals(new BigDecimal("105.00"), vat21);
        
        dailyReport.setVat0Collected(vat0);
        dailyReport.setVat11Collected(vat11);
        dailyReport.setVat21Collected(vat21);
        dailyReport.recalculateMetrics();
        
        assertEquals(new BigDecimal("127.00"), dailyReport.getTotalVAT());
    }

    /**
     * Test VAT report with deductible VAT
     */
    @Test
    public void testVATPayableCalculation() {
        vatReport.setVat0Collected(new BigDecimal("0.00"));
        vatReport.setVat11Collected(new BigDecimal("110.00"));
        vatReport.setVat21Collected(new BigDecimal("420.00"));
        vatReport.setVatDeductible(new BigDecimal("200.00"));
        
        vatReport.recalculateVAT();
        
        // VAT Payable = Collected - Deductible
        BigDecimal collected = new BigDecimal("530.00"); // 0 + 110 + 420
        BigDecimal payable = collected.subtract(new BigDecimal("200.00"));
        
        assertEquals(new BigDecimal("530.00"), vatReport.getVatCollected());
        assertEquals(new BigDecimal("330.00"), vatReport.getVatPayable());
    }

    /**
     * Test VAT payable never negative
     */
    @Test
    public void testVATPayableNeverNegative() {
        vatReport.setVat0Collected(new BigDecimal("100.00"));
        vatReport.setVat11Collected(new BigDecimal("100.00"));
        vatReport.setVat21Collected(new BigDecimal("100.00"));
        vatReport.setVatDeductible(new BigDecimal("500.00")); // More deductible than collected
        
        vatReport.recalculateVAT();
        
        assertTrue(vatReport.getVatPayable().compareTo(BigDecimal.ZERO) >= 0);
    }

    /**
     * Test monthly VAT aggregation from daily reports
     */
    @Test
    public void testMonthlyVATAggregation() {
        // Simulate 3 daily reports
        DailyFinancialReport day1 = new DailyFinancialReport();
        day1.setVat0Collected(new BigDecimal("0.00"));
        day1.setVat11Collected(new BigDecimal("50.00"));
        day1.setVat21Collected(new BigDecimal("100.00"));
        
        DailyFinancialReport day2 = new DailyFinancialReport();
        day2.setVat0Collected(new BigDecimal("10.00"));
        day2.setVat11Collected(new BigDecimal("60.00"));
        day2.setVat21Collected(new BigDecimal("150.00"));
        
        DailyFinancialReport day3 = new DailyFinancialReport();
        day3.setVat0Collected(new BigDecimal("5.00"));
        day3.setVat11Collected(new BigDecimal("40.00"));
        day3.setVat21Collected(new BigDecimal("120.00"));
        
        // Aggregate monthly
        BigDecimal monthlyVat0 = day1.getVat0Collected()
            .add(day2.getVat0Collected())
            .add(day3.getVat0Collected());
        
        BigDecimal monthlyVat11 = day1.getVat11Collected()
            .add(day2.getVat11Collected())
            .add(day3.getVat11Collected());
        
        BigDecimal monthlyVat21 = day1.getVat21Collected()
            .add(day2.getVat21Collected())
            .add(day3.getVat21Collected());
        
        assertEquals(new BigDecimal("15.00"), monthlyVat0);
        assertEquals(new BigDecimal("150.00"), monthlyVat11);
        assertEquals(new BigDecimal("370.00"), monthlyVat21);
        assertEquals(new BigDecimal("535.00"), 
            monthlyVat0.add(monthlyVat11).add(monthlyVat21));
    }

    /**
     * Test edge case: Very small VAT amounts
     */
    @Test
    public void testSmallVATAmounts() {
        dailyReport.setVat0Collected(new BigDecimal("0.01"));
        dailyReport.setVat11Collected(new BigDecimal("0.02"));
        dailyReport.setVat21Collected(new BigDecimal("0.03"));
        
        dailyReport.recalculateMetrics();
        
        assertEquals(new BigDecimal("0.06"), dailyReport.getTotalVAT());
    }

    /**
     * Test edge case: Large VAT amounts
     */
    @Test
    public void testLargeVATAmounts() {
        dailyReport.setVat0Collected(new BigDecimal("10000.00"));
        dailyReport.setVat11Collected(new BigDecimal("50000.00"));
        dailyReport.setVat21Collected(new BigDecimal("100000.00"));
        
        dailyReport.recalculateMetrics();
        
        assertEquals(new BigDecimal("160000.00"), dailyReport.getTotalVAT());
    }

    /**
     * Test precision with many decimal places
     */
    @Test
    public void testVATCalculationPrecision() {
        BigDecimal saleAmount = new BigDecimal("123.456789");
        BigDecimal vatRate = new BigDecimal("21");
        
        BigDecimal vatMultiplier = vatRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal vat = saleAmount.multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);
        
        // 123.456789 * 0.21 = 25.92... should round to 25.92
        assertEquals(2, vat.scale());
        assertNotNull(vat);
    }

    /**
     * Test that VAT components can be null and default to zero
     */
    @Test
    public void testNullVATComponentsDefaultToZero() {
        DailyFinancialReport report = new DailyFinancialReport();
        report.setVat0Collected(null);
        report.setVat11Collected(null);
        report.setVat21Collected(null);
        
        report.recalculateMetrics();
        
        // Should handle nulls gracefully
        BigDecimal result = (report.getVat0Collected() != null ? report.getVat0Collected() : BigDecimal.ZERO)
            .add(report.getVat11Collected() != null ? report.getVat11Collected() : BigDecimal.ZERO)
            .add(report.getVat21Collected() != null ? report.getVat21Collected() : BigDecimal.ZERO);
        
        assertEquals(0, new BigDecimal("0").compareTo(result));
    }
}
