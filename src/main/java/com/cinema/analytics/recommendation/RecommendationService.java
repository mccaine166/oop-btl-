package com.cinema.analytics.recommendation;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private final List<RecommendationStrategy> strategies;

    public RecommendationService(List<RecommendationStrategy> strategies) {
        this.strategies = strategies;
    }

    public List<Recommendation> getRecommendations(LocalDate weekStart, LocalDate weekEnd) {
        return getByTypes(EnumSet.allOf(RecommendationType.class), weekStart, weekEnd);
    }

    public List<Recommendation> getByType(RecommendationType type, LocalDate weekStart, LocalDate weekEnd) {
        return getByTypes(EnumSet.of(type), weekStart, weekEnd);
    }

    public List<Recommendation> getByTypes(Set<RecommendationType> types, LocalDate weekStart, LocalDate weekEnd) {
        if (types == null || types.isEmpty()) {
            return List.of();
        }

        return strategies.stream()
                .filter(strategy -> strategy != null && strategy.supportedTypes() != null)
                .filter(strategy -> strategy.supportedTypes().stream().anyMatch(types::contains))
                .flatMap(strategy -> strategy.recommend(weekStart, weekEnd).stream())
                .filter(Objects::nonNull)
                .filter(recommendation -> recommendation.getType() != null && types.contains(recommendation.getType()))
                .sorted(Comparator.comparingInt(Recommendation::getPriority).reversed())
                .collect(Collectors.toList());
    }
}
