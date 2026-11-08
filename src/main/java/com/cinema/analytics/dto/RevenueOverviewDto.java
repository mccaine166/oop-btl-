package com.cinema.analytics.dto;

import java.math.BigDecimal;

public record RevenueOverviewDto(
        BigDecimal totalRevenue,
        BigDecimal ticketRevenue,
        BigDecimal foodRevenue,
        BigDecimal averageTransactionValue,
        BigDecimal ticketRevenueShare,
        BigDecimal foodRevenueShare
) {
}
