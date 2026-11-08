package com.cinema.analytics.recommendation;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface RecommendationStrategy {
    Set<RecommendationType> supportedTypes();

    List<Recommendation> recommend(LocalDate weekStart, LocalDate weekEnd);
}
