package com.cinema.analytics.dto;

import java.math.BigDecimal;

public record InventorySummaryDto(
        String productId,
        String productName,
        int currentStock,
        BigDecimal consumptionRate,
        BigDecimal daysOfStockRemaining
) {
}
