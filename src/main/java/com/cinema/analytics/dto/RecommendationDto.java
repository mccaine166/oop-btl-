package com.cinema.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecommendationDto(
        String id,
        String type,
        String targetId,
        String targetName,
        String action,
        String reason,
        LocalDate analysisWeekStart,
        LocalDate analysisWeekEnd,
        LocalDate applyWeekStart,
        LocalDate applyWeekEnd,
        BigDecimal confidence,
        int priority
) {
}
