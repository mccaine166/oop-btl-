package com.cinema.analytics.dto;

import com.cinema.analytics.model.ProductType;

import java.math.BigDecimal;

public record FnbSummaryDto(
        String productId,
        String productName,
        int quantity,
        BigDecimal revenue,
        BigDecimal averageSalesPerDay,
        BigDecimal consumptionRate,
        BigDecimal daysOfStockRemaining,
        ProductType productType
) {
}
