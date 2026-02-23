package com.bakerymanager.service;

import com.bakerymanager.repository.UnitConversionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class UnitConversionService {

    private final UnitConversionRepository unitConversionRepository;

    private static final Map<String, BigDecimal> DEFAULT_CONVERSIONS = new HashMap<>();

    static {
        DEFAULT_CONVERSIONS.put(key("KG", "G"), new BigDecimal("1000"));
        DEFAULT_CONVERSIONS.put(key("G", "KG"), new BigDecimal("0.001"));
        DEFAULT_CONVERSIONS.put(key("KG", "GRAM"), new BigDecimal("1000"));
        DEFAULT_CONVERSIONS.put(key("GRAM", "KG"), new BigDecimal("0.001"));
        DEFAULT_CONVERSIONS.put(key("L", "ML"), new BigDecimal("1000"));
        DEFAULT_CONVERSIONS.put(key("ML", "L"), new BigDecimal("0.001"));
        DEFAULT_CONVERSIONS.put(key("BAX", "BUC"), new BigDecimal("10"));
        DEFAULT_CONVERSIONS.put(key("BUC", "BAX"), new BigDecimal("0.1"));
    }

    public UnitConversionService(UnitConversionRepository unitConversionRepository) {
        this.unitConversionRepository = unitConversionRepository;
    }

    public BigDecimal convert(BigDecimal quantity, String fromUnit, String toUnit) {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }

        String normalizedFrom = normalize(fromUnit);
        String normalizedTo = normalize(toUnit);

        if (normalizedFrom.equals(normalizedTo)) {
            return quantity;
        }

        return unitConversionRepository.findByFromUnitAndToUnit(normalizedFrom, normalizedTo)
            .map(conversion -> quantity.multiply(conversion.getFactor()))
            .orElseGet(() -> {
                BigDecimal factor = DEFAULT_CONVERSIONS.get(key(normalizedFrom, normalizedTo));
                if (factor == null) {
                    throw new IllegalArgumentException("Nu există conversie pentru " + normalizedFrom + " -> " + normalizedTo);
                }
                return quantity.multiply(factor);
            });
    }

    private static String normalize(String unit) {
        if (unit == null) {
            return "";
        }
        String normalized = unit.trim().toUpperCase();
        if ("GRAM".equals(normalized) || "GR".equals(normalized) || "G".equals(normalized)) {
            return "G";
        }
        if ("MILILITRU".equals(normalized) || "ML".equals(normalized)) {
            return "ML";
        }
        if ("LITRU".equals(normalized) || "L".equals(normalized)) {
            return "L";
        }
        return normalized;
    }

    private static String key(String from, String to) {
        return from + "->" + to;
    }
}
