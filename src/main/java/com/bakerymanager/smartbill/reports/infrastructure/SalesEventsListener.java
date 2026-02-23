package com.bakerymanager.smartbill.reports.infrastructure;

import com.bakerymanager.smartbill.sales.domain.event.SaleCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SalesEventsListener {

    private static final Logger logger = LoggerFactory.getLogger(SalesEventsListener.class);

    @EventListener
    public void onSaleCompleted(SaleCompletedEvent event) {
        logger.info("[Reports] Sale completed event received: saleId={}, total={}, paymentMethod={}, at={}",
            event.saleId(), event.totalAmount(), event.paymentMethod(), event.completedAt());
    }
}
