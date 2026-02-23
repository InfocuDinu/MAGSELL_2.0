package com.bakerymanager.smartbill.sales.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleCompletedEvent(
    Long saleId,
    BigDecimal totalAmount,
    String paymentMethod,
    LocalDateTime completedAt
) {
}
