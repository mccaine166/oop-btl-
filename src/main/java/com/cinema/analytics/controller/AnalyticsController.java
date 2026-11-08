package com.cinema.analytics.controller;

import com.cinema.analytics.dto.DailyRevenueDto;
import com.cinema.analytics.dto.FnbSummaryDto;
import com.cinema.analytics.dto.InventorySummaryDto;
import com.cinema.analytics.dto.MovieRevenueDto;
import com.cinema.analytics.dto.RecommendationDto;
import com.cinema.analytics.dto.RevenueOverviewDto;
import com.cinema.analytics.recommendation.RecommendationType;
import com.cinema.analytics.recommendation.RecommendationService;
import com.cinema.analytics.service.AnalyticsMapper;
import com.cinema.analytics.service.RevenueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class AnalyticsController {
    private final RevenueService revenueService;
    private final RecommendationService recommendationService;
    private final AnalyticsMapper analyticsMapper;

    public AnalyticsController(RevenueService revenueService,
                              RecommendationService recommendationService,
                              AnalyticsMapper analyticsMapper) {
        this.revenueService = revenueService;
        this.recommendationService = recommendationService;
        this.analyticsMapper = analyticsMapper;
    }

    @GetMapping("/analytics/revenue")
    public ResponseEntity<RevenueOverviewDto> getRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        validateRange(weekStart, weekEnd);
        RevenueOverviewDto payload = analyticsMapper.toRevenueOverview(
                revenueService.getTotalRevenue(weekStart, weekEnd),
                revenueService.getTicketRevenue(weekStart, weekEnd),
                revenueService.getFoodRevenue(weekStart, weekEnd),
                revenueService.getAverageTransactionValue(weekStart, weekEnd),
                revenueService.getTicketRevenueShare(weekStart, weekEnd),
                revenueService.getFoodRevenueShare(weekStart, weekEnd)
        );
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/analytics/revenue/daily")
    public ResponseEntity<List<DailyRevenueDto>> getDailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        validateRange(weekStart, weekEnd);
        return ResponseEntity.ok(analyticsMapper.toDailyRevenueList(revenueService.getDailyRevenue(weekStart, weekEnd)));
    }

    @GetMapping("/analytics/revenue/movie")
    public ResponseEntity<List<MovieRevenueDto>> getMovieRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        validateRange(weekStart, weekEnd);
        return ResponseEntity.ok(analyticsMapper.toMovieRevenueList(revenueService.getRevenueByMovie(weekStart, weekEnd)));
    }

    @GetMapping("/analytics/fnb")
    public ResponseEntity<List<FnbSummaryDto>> getFnb(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        validateRange(weekStart, weekEnd);
        return ResponseEntity.ok(analyticsMapper.toFnbSummaryList(weekStart, weekEnd));
    }

    @GetMapping("/analytics/inventory")
    public ResponseEntity<List<InventorySummaryDto>> getInventory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        validateRange(weekStart, weekEnd);
        return ResponseEntity.ok(analyticsMapper.toInventorySummaryList(weekStart, weekEnd));
    }

    @GetMapping("/recommendations/showtimes")
    public ResponseEntity<List<RecommendationDto>> getShowtimeRecommendations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        validateRange(weekStart, weekEnd);
        var recommendations = recommendationService.getByTypes(EnumSet.of(
                RecommendationType.INCREASE_SHOWTIME,
                RecommendationType.DECREASE_SHOWTIME
        ), weekStart, weekEnd);
        return ResponseEntity.ok(analyticsMapper.toRecommendationDtoList(recommendations));
    }

    @GetMapping("/recommendations/inventory")
    public ResponseEntity<List<RecommendationDto>> getInventoryRecommendations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        validateRange(weekStart, weekEnd);
        return ResponseEntity.ok(analyticsMapper.toRecommendationDtoList(
                recommendationService.getByType(RecommendationType.RESTOCK, weekStart, weekEnd)
        ));
    }

    private void validateRange(LocalDate weekStart, LocalDate weekEnd) {
        if (weekEnd.isBefore(weekStart)) {
            throw new IllegalArgumentException("weekEnd must be on or after weekStart");
        }
    }
}
