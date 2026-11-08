package com.cinema.analytics.recommendation.strategy;

import com.cinema.analytics.config.RecommendationProperties;
import com.cinema.analytics.recommendation.Recommendation;
import com.cinema.analytics.recommendation.RecommendationStrategy;
import com.cinema.analytics.recommendation.RecommendationType;
import com.cinema.analytics.recommendation.ShowtimeOccupancy;
import com.cinema.analytics.recommendation.ShowtimeStatsProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ShowtimeRecommendationStrategy implements RecommendationStrategy {
    private final ShowtimeStatsProvider showtimeStatsProvider;
    private final RecommendationProperties recommendationProperties;

    public ShowtimeRecommendationStrategy(ShowtimeStatsProvider showtimeStatsProvider,
                                         RecommendationProperties recommendationProperties) {
        this.showtimeStatsProvider = showtimeStatsProvider;
        this.recommendationProperties = recommendationProperties;
    }

    @Override
    public Set<RecommendationType> supportedTypes() {
        return EnumSet.of(RecommendationType.INCREASE_SHOWTIME, RecommendationType.DECREASE_SHOWTIME);
    }

    @Override
    public List<Recommendation> recommend(LocalDate weekStart, LocalDate weekEnd) {
        List<ShowtimeOccupancy> occupancySnapshot = showtimeStatsProvider.getOccupancySnapshot(weekStart, weekEnd);
        if (occupancySnapshot == null || occupancySnapshot.isEmpty()) {
            return List.of();
        }

        Map<String, List<ShowtimeOccupancy>> groupedByMovieAndTime = occupancySnapshot.stream()
                .collect(Collectors.groupingBy(
                        occupancy -> occupancy.movieId() + "|" + occupancy.timeSlot(),
                        Collectors.toList()
                ));

        long analysisDays = Math.max(1, ChronoUnit.DAYS.between(weekStart, weekEnd) + 1);
        LocalDate applyWeekStart = weekEnd.plusDays(1);
        LocalDate applyWeekEnd = applyWeekStart.plusDays(analysisDays - 1);

        return groupedByMovieAndTime.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= recommendationProperties.getMinSampleSize())
                .map(entry -> buildRecommendation(entry.getKey(), entry.getValue(), weekStart, weekEnd, applyWeekStart, applyWeekEnd))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(Recommendation::getPriority).reversed())
                .collect(Collectors.toList());
    }

    private Recommendation buildRecommendation(String groupKey,
                                              List<ShowtimeOccupancy> occupancyGroup,
                                              LocalDate weekStart,
                                              LocalDate weekEnd,
                                              LocalDate applyWeekStart,
                                              LocalDate applyWeekEnd) {
        String movieId = groupKey.split("\\|")[0];
        String timeSlot = groupKey.split("\\|")[1];
        double averageOccupancy = occupancyGroup.stream()
                .mapToDouble(ShowtimeOccupancy::occupancy)
                .average()
                .orElse(0.0d);
        long aboveHighThreshold = occupancyGroup.stream()
                .filter(item -> item.occupancy() >= recommendationProperties.getOccupancyHighThreshold())
                .count();
        long belowLowThreshold = occupancyGroup.stream()
                .filter(item -> item.occupancy() <= recommendationProperties.getOccupancyLowThreshold())
                .count();

        if (averageOccupancy >= recommendationProperties.getOccupancyHighThreshold()) {
            double distanceToThreshold = averageOccupancy - recommendationProperties.getOccupancyHighThreshold();
            double scale = Math.max(0.05d, 1.0d - recommendationProperties.getOccupancyHighThreshold());
            int increaseCount = Math.max(1, (int) Math.ceil((distanceToThreshold / scale) * 2.0d));
            BigDecimal confidence = BigDecimal.valueOf(Math.min(0.96d, 0.55d + (distanceToThreshold / scale) * 0.4d))
                    .setScale(2, RoundingMode.HALF_UP);
            String action = "Tăng " + increaseCount + " suất phim " + movieLabel(movieId) + " khung " + timeSlot;
            String reason = "Trên " + occupancyGroup.size() + " suất chiếu trong tuần (từ " + weekStart + " đến " + weekEnd + "), occupancy trung bình "
                    + formatPercent(averageOccupancy) + ", có " + aboveHighThreshold + " suất vượt ngưỡng cao "
                    + formatPercent(recommendationProperties.getOccupancyHighThreshold()) + ".";
            String id = "showtime-increase-" + movieId + "-" + slugTimeSlot(timeSlot) + "-" + weekStart;
            return new Recommendation(
                    id,
                    RecommendationType.INCREASE_SHOWTIME,
                    movieId,
                    movieLabel(movieId) + " (" + timeSlot + ")",
                    action,
                    reason,
                    weekStart,
                    weekEnd,
                    applyWeekStart,
                    applyWeekEnd,
                    confidence,
                    3
            );
        }

        if (averageOccupancy <= recommendationProperties.getOccupancyLowThreshold()) {
            double distanceToThreshold = recommendationProperties.getOccupancyLowThreshold() - averageOccupancy;
            double scale = Math.max(0.05d, recommendationProperties.getOccupancyLowThreshold());
            int decreaseCount = Math.max(1, (int) Math.ceil((distanceToThreshold / scale) * 2.0d));
            BigDecimal confidence = BigDecimal.valueOf(Math.min(0.96d, 0.55d + (distanceToThreshold / scale) * 0.4d))
                    .setScale(2, RoundingMode.HALF_UP);
            String action = "Giảm " + decreaseCount + " suất phim " + movieLabel(movieId) + " khung " + timeSlot;
            String reason = "Trên " + occupancyGroup.size() + " suất chiếu trong tuần (từ " + weekStart + " đến " + weekEnd + "), occupancy trung bình "
                    + formatPercent(averageOccupancy) + ", có " + belowLowThreshold + " suất dưới ngưỡng thấp "
                    + formatPercent(recommendationProperties.getOccupancyLowThreshold()) + ".";
            String id = "showtime-decrease-" + movieId + "-" + slugTimeSlot(timeSlot) + "-" + weekStart;
            return new Recommendation(
                    id,
                    RecommendationType.DECREASE_SHOWTIME,
                    movieId,
                    movieLabel(movieId) + " (" + timeSlot + ")",
                    action,
                    reason,
                    weekStart,
                    weekEnd,
                    applyWeekStart,
                    applyWeekEnd,
                    confidence,
                    2
            );
        }

        return null;
    }

    private String slugTimeSlot(String timeSlot) {
        if (timeSlot == null) {
            return "slot";
        }
        String withoutSymbols = timeSlot.replace(":", "")
                .replace("–", "-")
                .replace("—", "-")
                .replace("-", "-")
                .replaceAll("[^A-Za-z0-9\\-]", "");
        return withoutSymbols.replaceAll("-+", "-").replaceAll("^-|-$", "");
    }

    private String movieLabel(String movieId) {
        return movieId != null && movieId.startsWith("movie-") ? "Movie " + movieId.replace("movie-", "") : movieId;
    }

    private String formatPercent(double value) {
        return BigDecimal.valueOf(value * 100)
                .setScale(1, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }
}
