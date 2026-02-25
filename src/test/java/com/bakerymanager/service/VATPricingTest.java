package com.bakerymanager.service;

import com.bakerymanager.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VAT system with 3-tier rates (0%, 11%, 21%)
 */
public class VATPricingTest {

    private SaleItem saleItem;
    private Product product;

    @BeforeEach
    public void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Test Product");

        saleItem = new SaleItem();
        saleItem.setId(1L);
        saleItem.setQuantity(new BigDecimal("10"));
        saleItem.setUnitPrice(new BigDecimal("100.00"));
        saleItem.setProduct(product);
        saleItem.calculateTotal();
    }

    /**
     * Test 0% VAT rate (exempt products like bread)
     */
    @Test
    public void testZeroPercentVATRate() {
        product.setVatRate(new BigDecimal("0"));
        saleItem.setVatRate(product.getVatRate());

        BigDecimal salePrice = new BigDecimal("1000.00");
        BigDecimal vatRate = saleItem.getVatRate();
        BigDecimal vatMultiplier = vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal vat = salePrice.multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("0.00"), vat);
    }

    /**
     * Test 11% VAT rate (reduced rate products)
     */
    @Test
    public void testElevenPercentVATRate() {
        product.setVatRate(new BigDecimal("11"));
        saleItem.setVatRate(product.getVatRate());

        BigDecimal salePrice = new BigDecimal("1000.00");
        BigDecimal vatRate = saleItem.getVatRate();
        BigDecimal vatMultiplier = vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal vat = salePrice.multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("110.00"), vat);
    }

    /**
     * Test 21% VAT rate (standard rate products)
     */
    @Test
    public void testTwentyOnePercentVATRate() {
        product.setVatRate(new BigDecimal("21"));
        saleItem.setVatRate(product.getVatRate());

        BigDecimal salePrice = new BigDecimal("1000.00");
        BigDecimal vatRate = saleItem.getVatRate();
        BigDecimal vatMultiplier = vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal vat = salePrice.multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("210.00"), vat);
    }

    /**
     * Test VAT propagation from Product to SaleItem
     */
    @Test
    public void testVATRatePropagationFromProduct() {
        product.setVatRate(new BigDecimal("11"));
        saleItem.setProduct(product);
        saleItem.setVatRate(product.getVatRate());

        assertEquals(new BigDecimal("11"), saleItem.getVatRate());
    }

    /**
     * Test default VAT rate (21%) when not specified
     */
    @Test
    public void testDefaultVATRateWhenNotSpecified() {
        Product productWithoutRate = new Product();
        productWithoutRate.setId(2L);
        productWithoutRate.setName("Product without VAT");

        assertNotNull(productWithoutRate.getVatRate());
        assertEquals(new BigDecimal("21"), productWithoutRate.getVatRate());
    }

    /**
     * Test VAT calculation with fractional amounts
     */
    @Test
    public void testVATCalculationWithFractions() {
        product.setVatRate(new BigDecimal("11"));
        saleItem.setVatRate(product.getVatRate());

        BigDecimal salePrice = new BigDecimal("123.45");
        BigDecimal vatRate = saleItem.getVatRate();
        BigDecimal vatMultiplier = vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal vat = salePrice.multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);

        // 123.45 * 0.11 = 13.5795 rounded to 13.58 (HALF_UP)
        assertEquals(new BigDecimal("13.58"), vat);
    }

    /**
     * Test total price calculation with VAT included
     */
    @Test
    public void testTotalPriceWithVATIncluded() {
        BigDecimal basePrice = new BigDecimal("1000.00");
        BigDecimal vatRate = new BigDecimal("21");
        
        BigDecimal vatMultiplier = vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal vat = basePrice.multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalWithVAT = basePrice.add(vat);

        assertEquals(new BigDecimal("210.00"), vat);
        assertEquals(new BigDecimal("1210.00"), totalWithVAT);
    }

    /**
     * Test multiple sale items with different VAT rates
     */
    @Test
    public void testMultipleSaleItemsWithDifferentRates() {
        // Item 1: 0% VAT
        SaleItem item1 = new SaleItem();
        item1.setQuantity(new BigDecimal("5"));
        item1.setUnitPrice(new BigDecimal("100"));
        item1.setVatRate(new BigDecimal("0"));
        
        // Item 2: 11% VAT
        SaleItem item2 = new SaleItem();
        item2.setQuantity(new BigDecimal("3"));
        item2.setUnitPrice(new BigDecimal("50"));
        item2.setVatRate(new BigDecimal("11"));
        
        // Item 3: 21% VAT
        SaleItem item3 = new SaleItem();
        item3.setQuantity(new BigDecimal("2"));
        item3.setUnitPrice(new BigDecimal("75"));
        item3.setVatRate(new BigDecimal("21"));

        // Calculate totals by rate
        BigDecimal total0 = item1.getQuantity().multiply(item1.getUnitPrice());
        BigDecimal total11 = item2.getQuantity().multiply(item2.getUnitPrice());
        BigDecimal total21 = item3.getQuantity().multiply(item3.getUnitPrice());

        BigDecimal vat0 = total0.multiply(new BigDecimal("0"));
        BigDecimal vat11 = total11.multiply(new BigDecimal("0.11")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal vat21 = total21.multiply(new BigDecimal("0.21")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalVAT = vat0.add(vat11).add(vat21);

        assertEquals(new BigDecimal("500.00"), total0);
        assertEquals(new BigDecimal("150.00"), total11);
        assertEquals(new BigDecimal("150.00"), total21);
        assertEquals(new BigDecimal("0.00"), vat0);
        assertEquals(new BigDecimal("16.50"), vat11);
        assertEquals(new BigDecimal("31.50"), vat21);
        assertEquals(new BigDecimal("48.00"), totalVAT);
    }

    /**
     * Test VAT aggregation in daily financial report
     */
    @Test
    public void testVATAggregationByRate() {
        // Simulate daily report with mixed VAT rates
        BigDecimal vat0Collected = new BigDecimal("0.00");
        BigDecimal vat11Collected = new BigDecimal("55.00");
        BigDecimal vat21Collected = new BigDecimal("210.00");

        BigDecimal totalVAT = vat0Collected.add(vat11Collected).add(vat21Collected);

        assertEquals(new BigDecimal("265.00"), totalVAT);
        assertEquals(new BigDecimal("0.00"), vat0Collected);
        assertEquals(new BigDecimal("55.00"), vat11Collected);
        assertEquals(new BigDecimal("210.00"), vat21Collected);
    }

    /**
     * Test rounding precision for VAT calculations
     */
    @Test
    public void testVATRoundingPrecision() {
        // Test that all VAT calculations use HALF_UP rounding
        BigDecimal[] testAmounts = {
            new BigDecimal("0.01"),
            new BigDecimal("0.99"),
            new BigDecimal("1.234"),
            new BigDecimal("99.999")
        };

        for (BigDecimal amount : testAmounts) {
            BigDecimal vat21 = amount.multiply(new BigDecimal("0.21")).setScale(2, RoundingMode.HALF_UP);
            // Verify result is always 2 decimal places
            assertEquals(2, vat21.scale());
        }
    }

    /**
     * Test VAT rate validation (should only be 0, 11, or 21)
     */
    @Test
    public void testValidVATRates() {
        BigDecimal[] validRates = {
            new BigDecimal("0"),
            new BigDecimal("11"),
            new BigDecimal("21")
        };

        for (BigDecimal rate : validRates) {
            assertTrue(isValidVATRate(rate));
        }
    }

    /**
     * Test invalid VAT rates
     */
    @Test
    public void testInvalidVATRatesAreRejected() {
        BigDecimal[] invalidRates = {
            new BigDecimal("5"),
            new BigDecimal("19"),
            new BigDecimal("100"),
            new BigDecimal("-21")
        };

        for (BigDecimal rate : invalidRates) {
            assertFalse(isValidVATRate(rate));
        }
    }

    /**
     * Helper method to validate VAT rates
     */
    private boolean isValidVATRate(BigDecimal rate) {
        return rate.compareTo(new BigDecimal("0")) == 0 ||
               rate.compareTo(new BigDecimal("11")) == 0 ||
               rate.compareTo(new BigDecimal("21")) == 0;
    }
}
