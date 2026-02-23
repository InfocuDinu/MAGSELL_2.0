package com.bakerymanager.service;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.SaleItem;
import com.bakerymanager.repository.SaleItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ForecastService {

    private static final BigDecimal DEFAULT_UPLIFT = new BigDecimal("1.10");

    private final SaleItemRepository saleItemRepository;

    public ForecastService(SaleItemRepository saleItemRepository) {
        this.saleItemRepository = saleItemRepository;
    }

    public List<ForecastItem> getForecast(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(14);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay().minusSeconds(1);

        List<SaleItem> saleItems = saleItemRepository.findBySaleDateBetween(startDateTime, endDateTime);
        Map<Long, ForecastItem> byProduct = new HashMap<>();

        for (SaleItem item : saleItems) {
            if (item.getProduct() == null) {
                continue;
            }
            ForecastItem forecast = byProduct.computeIfAbsent(item.getProduct().getId(),
                id -> new ForecastItem(item.getProduct()));
            forecast.addQuantity(item.getQuantity());
        }

        long days = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1);
        List<ForecastItem> result = new ArrayList<>();
        for (ForecastItem item : byProduct.values()) {
            item.calculateAverage(days);
            result.add(item);
        }

        return result;
    }

    public static class ForecastItem {
        private final Product product;
        private BigDecimal totalQuantity = BigDecimal.ZERO;
        private BigDecimal averageDaily = BigDecimal.ZERO;
        private BigDecimal suggestedProduction = BigDecimal.ZERO;

        public ForecastItem(Product product) {
            this.product = product;
        }

        public void addQuantity(BigDecimal qty) {
            if (qty != null) {
                totalQuantity = totalQuantity.add(qty);
            }
        }

        public void calculateAverage(long days) {
            averageDaily = totalQuantity.divide(BigDecimal.valueOf(days), 3, RoundingMode.HALF_UP);
            suggestedProduction = averageDaily.multiply(DEFAULT_UPLIFT).setScale(3, RoundingMode.HALF_UP);
        }

        public Product getProduct() { return product; }
        public BigDecimal getTotalQuantity() { return totalQuantity; }
        public BigDecimal getAverageDaily() { return averageDaily; }
        public BigDecimal getSuggestedProduction() { return suggestedProduction; }
    }
}
