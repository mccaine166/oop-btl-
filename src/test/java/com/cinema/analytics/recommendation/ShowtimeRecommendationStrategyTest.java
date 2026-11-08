package com.cinema.analytics.recommendation;

import com.cinema.analytics.config.DataSeeder;
import com.cinema.analytics.config.RecommendationProperties;
import com.cinema.analytics.model.Ticket;
import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.recommendation.strategy.ShowtimeRecommendationStrategy;
import com.cinema.analytics.recommendation.stub.StubShowtimeStatsProvider;
import com.cinema.analytics.repository.memory.InMemoryInventoryRepository;
import com.cinema.analytics.repository.memory.InMemoryProductRepository;
import com.cinema.analytics.repository.memory.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowtimeRecommendationStrategyTest {
    private ShowtimeRecommendationStrategy strategy;

    @BeforeEach
    void setUp() {
        InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
        repository.saveAll(List.of(
                new Transaction(
                        "tx-1",
                        LocalDateTime.of(2026, 9, 18, 20, 0),
                        "show-1",
                        "movie-1",
                        List.of(
                                new Ticket("t-1", "A1", new BigDecimal("60000")),
                                new Ticket("t-2", "A2", new BigDecimal("60000")),
                                new Ticket("t-3", "A3", new BigDecimal("60000")),
                                new Ticket("t-4", "A4", new BigDecimal("60000")),
                                new Ticket("t-5", "A5", new BigDecimal("60000")),
                                new Ticket("t-6", "A6", new BigDecimal("60000")),
                                new Ticket("t-7", "A7", new BigDecimal("60000")),
                                new Ticket("t-8", "A8", new BigDecimal("60000")),
                                new Ticket("t-9", "A9", new BigDecimal("60000"))
                        ),
                        List.of(),
                        new BigDecimal("540000")
                ),
                new Transaction(
                        "tx-2",
                        LocalDateTime.of(2026, 9, 17, 12, 0),
                        "show-2",
                        "movie-2",
                        List.of(
                                new Ticket("t-10", "B1", new BigDecimal("50000")),
                                new Ticket("t-11", "B2", new BigDecimal("50000"))
                        ),
                        List.of(),
                        new BigDecimal("100000")
                )
        ));

        RecommendationProperties properties = new RecommendationProperties();
        properties.setOccupancyHighThreshold(0.7);
        properties.setOccupancyLowThreshold(0.5);
        properties.setMinSampleSize(1);

        strategy = new ShowtimeRecommendationStrategy(new StubShowtimeStatsProvider(repository), properties);
    }

    @Test
    void shouldGenerateIncreaseAndDecreaseRecommendations() {
        LocalDate start = LocalDate.of(2026, 9, 14);
        LocalDate end = LocalDate.of(2026, 9, 20);

        List<Recommendation> recommendations = strategy.recommend(start, end);

        assertTrue(recommendations.stream().anyMatch(recommendation -> recommendation.getType() == RecommendationType.INCREASE_SHOWTIME));
        assertTrue(recommendations.stream().anyMatch(recommendation -> recommendation.getType() == RecommendationType.DECREASE_SHOWTIME));
    }

    @Test
    void shouldIgnoreSmallSampleGroupsAndKeepSeedDataThresholds() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();
        InMemoryInventoryRepository inventoryRepository = new InMemoryInventoryRepository();
        new DataSeeder(productRepository, transactionRepository, inventoryRepository).seedData();

        RecommendationProperties properties = new RecommendationProperties();
        properties.setOccupancyHighThreshold(0.75);
        properties.setOccupancyLowThreshold(0.45);
        properties.setMinSampleSize(3);

        ShowtimeRecommendationStrategy seededStrategy = new ShowtimeRecommendationStrategy(
                new StubShowtimeStatsProvider(transactionRepository),
                properties
        );

        LocalDate start = LocalDate.of(2026, 9, 14);
        LocalDate end = LocalDate.of(2026, 9, 20);

        List<Recommendation> recommendations = seededStrategy.recommend(start, end);

        assertTrue(recommendations.stream().filter(r -> r.getType() == RecommendationType.INCREASE_SHOWTIME).count() >= 2);
        assertTrue(recommendations.stream().filter(r -> r.getType() == RecommendationType.DECREASE_SHOWTIME).count() >= 1);
    }

    @Test
    void shouldSkipGroupsBelowMinimumSampleSize() {
        InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
        repository.saveAll(List.of(
                new Transaction(
                        "low-1",
                        LocalDateTime.of(2026, 9, 14, 20, 0),
                        "show-low-1",
                        "movie-9",
                        List.of(new Ticket("t-1", "A1", new BigDecimal("60000"))),
                        List.of(),
                        new BigDecimal("60000")
                ),
                new Transaction(
                        "low-2",
                        LocalDateTime.of(2026, 9, 15, 20, 0),
                        "show-low-2",
                        "movie-9",
                        List.of(new Ticket("t-2", "A2", new BigDecimal("60000"))),
                        List.of(),
                        new BigDecimal("60000")
                )
        ));

        RecommendationProperties properties = new RecommendationProperties();
        properties.setOccupancyHighThreshold(0.75);
        properties.setOccupancyLowThreshold(0.45);
        properties.setMinSampleSize(3);

        ShowtimeRecommendationStrategy smallGroupStrategy = new ShowtimeRecommendationStrategy(
                new StubShowtimeStatsProvider(repository),
                properties
        );

        List<Recommendation> recommendations = smallGroupStrategy.recommend(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));
        assertTrue(recommendations.stream().noneMatch(r -> r.getType() == RecommendationType.INCREASE_SHOWTIME));
        assertTrue(recommendations.stream().noneMatch(r -> r.getType() == RecommendationType.DECREASE_SHOWTIME));

        repository.save(new Transaction(
                "low-3",
                LocalDateTime.of(2026, 9, 16, 20, 0),
                "show-low-3",
                "movie-9",
                List.of(new Ticket("t-3", "A3", new BigDecimal("60000"))),
                List.of(),
                new BigDecimal("60000")
        ));

        List<Recommendation> sufficientRecommendations = smallGroupStrategy.recommend(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));
        assertTrue(sufficientRecommendations.stream().anyMatch(r -> r.getType() == RecommendationType.INCREASE_SHOWTIME)
                || sufficientRecommendations.stream().anyMatch(r -> r.getType() == RecommendationType.DECREASE_SHOWTIME));
    }
}
