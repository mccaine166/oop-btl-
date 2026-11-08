package com.cinema.analytics.service;

import com.cinema.analytics.dto.DailyRevenueDto;
import com.cinema.analytics.dto.FnbSummaryDto;
import com.cinema.analytics.dto.InventorySummaryDto;
import com.cinema.analytics.dto.MovieRevenueDto;
import com.cinema.analytics.dto.RecommendationDto;
import com.cinema.analytics.dto.RevenueOverviewDto;
import com.cinema.analytics.model.Product;
import com.cinema.analytics.recommendation.Recommendation;
import com.cinema.analytics.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class AnalyticsMapper {
    private final ProductRepository productRepository;
    private final FnbAnalyticsService fnbAnalyticsService;
    private final InventoryService inventoryService;

    public AnalyticsMapper(ProductRepository productRepository,
                           FnbAnalyticsService fnbAnalyticsService,
                           InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.fnbAnalyticsService = fnbAnalyticsService;
        this.inventoryService = inventoryService;
    }

    public RevenueOverviewDto toRevenueOverview(BigDecimal totalRevenue,
                                              BigDecimal ticketRevenue,
                                              BigDecimal foodRevenue,
                                              BigDecimal averageTransactionValue,
                                              BigDecimal ticketRevenueShare,
                                              BigDecimal foodRevenueShare) {
        return new RevenueOverviewDto(totalRevenue, ticketRevenue, foodRevenue,
                averageTransactionValue, ticketRevenueShare, foodRevenueShare);
    }

    public List<DailyRevenueDto> toDailyRevenueList(Map<LocalDate, BigDecimal> dailyRevenue) {
        return dailyRevenue.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DailyRevenueDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<MovieRevenueDto> toMovieRevenueList(Map<String, BigDecimal> revenueByMovie) {
        return revenueByMovie.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MovieRevenueDto(entry.getKey(), "Movie " + entry.getKey().replace("movie-", ""), entry.getValue()))
                .toList();
    }

    public List<FnbSummaryDto> toFnbSummaryList(LocalDate weekStart, LocalDate weekEnd) {
        Map<String, Integer> quantityByProduct = fnbAnalyticsService.getQuantitySoldByProduct(weekStart, weekEnd);
        Map<String, BigDecimal> revenueByProduct = fnbAnalyticsService.getRevenueByProduct(weekStart, weekEnd);
        return quantityByProduct.entrySet().stream()
                .map(entry -> toFnbSummary(entry.getKey(), entry.getValue(), revenueByProduct, weekStart, weekEnd))
                .sorted(Comparator.comparing(FnbSummaryDto::quantity).reversed())
                .toList();
    }

    public List<InventorySummaryDto> toInventorySummaryList(LocalDate weekStart, LocalDate weekEnd) {
        return productRepository.findAll().stream()
                .map(product -> new InventorySummaryDto(
                        product.getId(),
                        product.getName(),
                        inventoryService.getCurrentStock(product.getId(), weekEnd),
                        inventoryService.getConsumptionRate(product.getId(), weekStart, weekEnd),
                        inventoryService.getDaysOfStockRemaining(product.getId(), weekStart, weekEnd)))
                .sorted(Comparator.comparing(InventorySummaryDto::productId))
                .toList();
    }

    public List<RecommendationDto> toRecommendationDtoList(List<Recommendation> recommendations) {
        return recommendations.stream()
                .map(recommendation -> new RecommendationDto(
                        recommendation.getId(),
                        recommendation.getType().name(),
                        recommendation.getTargetId(),
                        recommendation.getTargetName(),
                        recommendation.getAction(),
                        recommendation.getReason(),
                        recommendation.getAnalysisWeekStart(),
                        recommendation.getAnalysisWeekEnd(),
                        recommendation.getApplyWeekStart(),
                        recommendation.getApplyWeekEnd(),
                        recommendation.getConfidence(),
                        recommendation.getPriority()))
                .toList();
    }

    private FnbSummaryDto toFnbSummary(String productId,
                                       int quantity,
                                       Map<String, BigDecimal> revenueByProduct,
                                       LocalDate weekStart,
                                       LocalDate weekEnd) {
        Product product = productRepository.findById(productId).orElse(null);
        String productName = product != null ? product.getName() : productId;
        return new FnbSummaryDto(
                productId,
                productName,
                quantity,
                revenueByProduct.getOrDefault(productId, BigDecimal.ZERO),
                fnbAnalyticsService.getAverageSalesPerDay(productId, weekStart, weekEnd),
                fnbAnalyticsService.getConsumptionRate(productId, weekStart, weekEnd),
                inventoryService.getDaysOfStockRemaining(productId, weekStart, weekEnd),
                product != null ? product.getType() : null
        );
    }
}
