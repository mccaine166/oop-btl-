package com.cinema.analytics.recommendation.strategy;

import com.cinema.analytics.config.RecommendationProperties;
import com.cinema.analytics.model.Product;
import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.recommendation.Recommendation;
import com.cinema.analytics.recommendation.RecommendationStrategy;
import com.cinema.analytics.recommendation.RecommendationType;
import com.cinema.analytics.repository.ProductRepository;
import com.cinema.analytics.repository.TransactionRepository;
import com.cinema.analytics.service.InventoryService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InventoryRecommendationStrategy implements RecommendationStrategy {
    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final InventoryService inventoryService;
    private final RecommendationProperties recommendationProperties;

    public InventoryRecommendationStrategy(ProductRepository productRepository,
                                          TransactionRepository transactionRepository,
                                          InventoryService inventoryService,
                                          RecommendationProperties recommendationProperties) {
        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
        this.inventoryService = inventoryService;
        this.recommendationProperties = recommendationProperties;
    }

    @Override
    public Set<RecommendationType> supportedTypes() {
        return EnumSet.of(RecommendationType.RESTOCK);
    }

    @Override
    public List<Recommendation> recommend(LocalDate weekStart, LocalDate weekEnd) {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return List.of();
        }

        long analysisDays = Math.max(1, ChronoUnit.DAYS.between(weekStart, weekEnd) + 1);
        LocalDate previousWeekEnd = weekStart.minusDays(1);
        LocalDate previousWeekStart = previousWeekEnd.minusDays(analysisDays - 1);
        LocalDate applyWeekStart = weekEnd.plusDays(1);
        LocalDate applyWeekEnd = applyWeekStart.plusDays(analysisDays - 1);

        Map<String, Integer> analysisSales = aggregateSalesByProduct(transactionRepository.findByDateRange(weekStart, weekEnd));
        Map<String, Integer> previousSales = aggregateSalesByProduct(transactionRepository.findByDateRange(previousWeekStart, previousWeekEnd));
        Map<String, Map<LocalDate, Integer>> dailySales = aggregateDailySalesByProduct(transactionRepository.findByDateRange(weekStart, weekEnd));

        List<Recommendation> recommendations = new ArrayList<>();
        for (Product product : products) {
            String productId = product.getId();
            int soldThisWeek = analysisSales.getOrDefault(productId, 0);
            if (soldThisWeek <= 0) {
                continue;
            }

            int soldPreviousWeek = previousSales.getOrDefault(productId, 0);
            double growth = soldPreviousWeek == 0 ? 0.0 : ((double) soldThisWeek - soldPreviousWeek) / soldPreviousWeek;
            double boundedGrowth = clamp(growth, recommendationProperties.getMaxDecline(), recommendationProperties.getMaxGrowth());

            double weekdayAverage = calculateAverageByDayType(dailySales.getOrDefault(productId, Map.of()), weekStart, weekEnd, false);
            double weekendAverage = calculateAverageByDayType(dailySales.getOrDefault(productId, Map.of()), weekStart, weekEnd, true);
            double overallAverage = soldThisWeek / (double) Math.max(1, analysisDays);
            if (weekdayAverage <= 0 && weekendAverage <= 0) {
                weekdayAverage = overallAverage;
                weekendAverage = overallAverage;
            }

            double forecastWithoutSafety = 0.0;
            for (LocalDate date = applyWeekStart; !date.isAfter(applyWeekEnd); date = date.plusDays(1)) {
                double basePerDay = isWeekend(date)
                        ? (weekendAverage > 0 ? weekendAverage : overallAverage)
                        : (weekdayAverage > 0 ? weekdayAverage : overallAverage);
                forecastWithoutSafety += basePerDay;
            }
            double forecastWithGrowth = forecastWithoutSafety * (1.0 + boundedGrowth);
            double safetyAdjustedForecast = forecastWithGrowth * (1.0 + recommendationProperties.getSafetyFactor());

            int currentStock = inventoryService.getCurrentStock(productId, weekEnd);
            int recommendedRestock = Math.max(0, (int) Math.ceil(safetyAdjustedForecast) - currentStock);
            if (recommendedRestock <= 0) {
                continue;
            }

            double cv = calculateCoefficientOfVariation(dailySales.getOrDefault(productId, Map.of()), weekStart, weekEnd);
            double confidence = clamp(
                    0.50 + (soldPreviousWeek > 0 ? 0.25 : 0.0) + 0.20 * (1.0 - Math.min(1.0, cv)),
                    0.50,
                    0.95
            );

            String trendText;
            if (soldPreviousWeek == 0) {
                trendText = "chưa có dữ liệu tuần trước nên không so sánh xu hướng";
            } else {
                String changePart = formatPercentage(growth * 100.0);
                if (Math.abs(growth - boundedGrowth) > 0.0001) {
                    changePart = formatPercentage(growth * 100.0) + " nhưng đã bị giới hạn ở " + formatPercentage(boundedGrowth * 100.0);
                }
                trendText = "tuần trước bán " + soldPreviousWeek + " đơn vị, tuần này bán " + soldThisWeek + " đơn vị, thay đổi " + changePart;
            }

            String reason = String.format(
                    "Tuần %s–%s bán %d %s (%s). Dự báo tuần %s–%s: %d đơn vị; cộng hệ số an toàn %d%% = %d; tồn kho hiện tại %d → cần nhập %d. Độ tin cậy %.2f: %s.",
                    weekStart,
                    weekEnd,
                    soldThisWeek,
                    product.getName(),
                    trendText,
                    applyWeekStart,
                    applyWeekEnd,
                    (int) Math.round(forecastWithoutSafety),
                    (int) Math.round(recommendationProperties.getSafetyFactor() * 100),
                    (int) Math.round(safetyAdjustedForecast),
                    currentStock,
                    recommendedRestock,
                    BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP),
                    soldPreviousWeek > 0
                            ? "có dữ liệu tuần trước, doanh số ngày biến động CV = " + formatRatio(cv)
                            : "chưa có dữ liệu tuần trước, doanh số ngày biến động CV = " + formatRatio(cv)
            );

            recommendations.add(new Recommendation(
                    "restock-" + productId + "-" + weekStart,
                    RecommendationType.RESTOCK,
                    productId,
                    product.getName(),
                    "Nhập thêm " + recommendedRestock + " " + product.getName(),
                    reason,
                    weekStart,
                    weekEnd,
                    applyWeekStart,
                    applyWeekEnd,
                    BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP),
                    5
            ));
        }

        return recommendations.stream()
                .sorted(Comparator.comparingInt(Recommendation::getPriority).reversed())
                .collect(Collectors.toList());
    }

    private Map<String, Integer> aggregateSalesByProduct(List<Transaction> transactions) {
        Map<String, Integer> totals = new HashMap<>();
        for (Transaction transaction : transactions) {
            for (Transaction.FnbItem item : transaction.getFoodItems()) {
                totals.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }
        return totals;
    }

    private Map<String, Map<LocalDate, Integer>> aggregateDailySalesByProduct(List<Transaction> transactions) {
        Map<String, Map<LocalDate, Integer>> result = new HashMap<>();
        for (Transaction transaction : transactions) {
            LocalDate date = transaction.getCreatedAt().toLocalDate();
            for (Transaction.FnbItem item : transaction.getFoodItems()) {
                result.computeIfAbsent(item.getProductId(), key -> new HashMap<>())
                        .merge(date, item.getQuantity(), Integer::sum);
            }
        }
        return result;
    }

    private double calculateAverageByDayType(Map<LocalDate, Integer> dailySales, LocalDate weekStart, LocalDate weekEnd, boolean weekend) {
        if (dailySales == null || dailySales.isEmpty()) {
            return 0.0;
        }

        List<Double> values = new ArrayList<>();
        LocalDate cursor = weekStart;
        while (!cursor.isAfter(weekEnd)) {
            int value = dailySales.getOrDefault(cursor, 0);
            if (isWeekend(cursor) == weekend && value > 0) {
                values.add((double) value);
            }
            cursor = cursor.plusDays(1);
        }

        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateCoefficientOfVariation(Map<LocalDate, Integer> dailySales, LocalDate weekStart, LocalDate weekEnd) {
        if (dailySales == null || dailySales.isEmpty()) {
            return 0.0;
        }

        List<Integer> values = new ArrayList<>();
        LocalDate cursor = weekStart;
        while (!cursor.isAfter(weekEnd)) {
            values.add(dailySales.getOrDefault(cursor, 0));
            cursor = cursor.plusDays(1);
        }

        if (values.isEmpty()) {
            return 0.0;
        }

        double mean = values.stream().mapToDouble(Integer::doubleValue).average().orElse(0.0);
        if (mean <= 0.0) {
            return 0.0;
        }
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance) / mean;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatPercentage(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    private String formatRatio(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
