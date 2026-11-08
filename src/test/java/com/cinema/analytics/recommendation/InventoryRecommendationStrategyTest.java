package com.cinema.analytics.recommendation;

import com.cinema.analytics.config.DataSeeder;
import com.cinema.analytics.config.RecommendationProperties;
import com.cinema.analytics.model.InventoryTransaction;
import com.cinema.analytics.model.InventoryTransactionType;
import com.cinema.analytics.model.Product;
import com.cinema.analytics.model.ProductType;
import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.recommendation.strategy.InventoryRecommendationStrategy;
import com.cinema.analytics.repository.memory.InMemoryInventoryRepository;
import com.cinema.analytics.repository.memory.InMemoryProductRepository;
import com.cinema.analytics.repository.memory.InMemoryTransactionRepository;
import com.cinema.analytics.service.FnbAnalyticsService;
import com.cinema.analytics.service.InventoryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryRecommendationStrategyTest {
    @Test
    void shouldRecommendRestockWithPositiveGrowthAndExactQuantity() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();
        InMemoryInventoryRepository inventoryRepository = new InMemoryInventoryRepository();
        Product product = new Product("prod-combo-a", "Combo A", ProductType.COMBO, new BigDecimal("129000"), "combo");
        productRepository.save(product);

        inventoryRepository.saveAll(List.of(
                new InventoryTransaction("inv-open", "prod-combo-a", InventoryTransactionType.IMPORT, 40, LocalDate.of(2026, 9, 7)),
                new InventoryTransaction("inv-export", "prod-combo-a", InventoryTransactionType.EXPORT, 15, LocalDate.of(2026, 9, 18))
        ));

        LocalDate weekStart = LocalDate.of(2026, 9, 14);
        LocalDate weekEnd = LocalDate.of(2026, 9, 20);

        transactionRepository.saveAll(List.of(
                new Transaction("prev-1", LocalDateTime.of(2026, 9, 7, 19, 0), "show-1", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 2, new BigDecimal("129000"))), new BigDecimal("258000")),
                new Transaction("prev-2", LocalDateTime.of(2026, 9, 8, 19, 0), "show-2", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 2, new BigDecimal("129000"))), new BigDecimal("258000")),
                new Transaction("prev-3", LocalDateTime.of(2026, 9, 9, 19, 0), "show-3", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 2, new BigDecimal("129000"))), new BigDecimal("258000")),
                new Transaction("prev-4", LocalDateTime.of(2026, 9, 10, 19, 0), "show-4", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 2, new BigDecimal("129000"))), new BigDecimal("258000")),
                new Transaction("prev-5", LocalDateTime.of(2026, 9, 11, 19, 0), "show-5", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 2, new BigDecimal("129000"))), new BigDecimal("258000")),
                new Transaction("prev-6", LocalDateTime.of(2026, 9, 12, 20, 0), "show-6", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 5, new BigDecimal("129000"))), new BigDecimal("645000")),
                new Transaction("prev-7", LocalDateTime.of(2026, 9, 13, 20, 0), "show-7", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 5, new BigDecimal("129000"))), new BigDecimal("645000")),
                new Transaction("curr-1", LocalDateTime.of(2026, 9, 14, 19, 0), "show-8", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 4, new BigDecimal("129000"))), new BigDecimal("516000")),
                new Transaction("curr-2", LocalDateTime.of(2026, 9, 15, 19, 0), "show-9", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 4, new BigDecimal("129000"))), new BigDecimal("516000")),
                new Transaction("curr-3", LocalDateTime.of(2026, 9, 16, 19, 0), "show-10", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 4, new BigDecimal("129000"))), new BigDecimal("516000")),
                new Transaction("curr-4", LocalDateTime.of(2026, 9, 17, 19, 0), "show-11", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 4, new BigDecimal("129000"))), new BigDecimal("516000")),
                new Transaction("curr-5", LocalDateTime.of(2026, 9, 18, 19, 0), "show-12", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 4, new BigDecimal("129000"))), new BigDecimal("516000")),
                new Transaction("curr-6", LocalDateTime.of(2026, 9, 19, 20, 0), "show-13", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 5, new BigDecimal("129000"))), new BigDecimal("645000")),
                new Transaction("curr-7", LocalDateTime.of(2026, 9, 20, 20, 0), "show-14", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-combo-a", 5, new BigDecimal("129000"))), new BigDecimal("645000"))
        ));

        RecommendationProperties properties = new RecommendationProperties();
        properties.setSafetyFactor(0.15);
        InventoryService inventoryService = new InventoryService(productRepository, inventoryRepository, new FnbAnalyticsService(transactionRepository, productRepository));
        InventoryRecommendationStrategy strategy = new InventoryRecommendationStrategy(productRepository, transactionRepository, inventoryService, properties);

        List<Recommendation> recommendations = strategy.recommend(weekStart, weekEnd);
        assertTrue(recommendations.stream().anyMatch(rec -> rec.getAction().startsWith("Nhập thêm ")));
        assertTrue(recommendations.get(0).getReason().contains("Dự báo tuần"));
    }

    @Test
    void shouldClampNegativeGrowthAtMaxDecline() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();
        InMemoryInventoryRepository inventoryRepository = new InMemoryInventoryRepository();
        Product product = new Product("prod-drink", "Drink", ProductType.SINGLE, new BigDecimal("45000"), "chai");
        productRepository.save(product);

        inventoryRepository.saveAll(List.of(
                new InventoryTransaction("open", "prod-drink", InventoryTransactionType.IMPORT, 20, LocalDate.of(2026, 9, 7)),
                new InventoryTransaction("exp", "prod-drink", InventoryTransactionType.EXPORT, 10, LocalDate.of(2026, 9, 18))
        ));

        LocalDate weekStart = LocalDate.of(2026, 9, 14);
        LocalDate weekEnd = LocalDate.of(2026, 9, 20);

        transactionRepository.saveAll(List.of(
                new Transaction("prev-1", LocalDateTime.of(2026, 9, 7, 18, 0), "show-1", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 5, new BigDecimal("45000"))), new BigDecimal("225000")),
                new Transaction("prev-2", LocalDateTime.of(2026, 9, 8, 18, 0), "show-2", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 5, new BigDecimal("45000"))), new BigDecimal("225000")),
                new Transaction("prev-3", LocalDateTime.of(2026, 9, 9, 18, 0), "show-3", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 5, new BigDecimal("45000"))), new BigDecimal("225000")),
                new Transaction("prev-4", LocalDateTime.of(2026, 9, 10, 18, 0), "show-4", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 5, new BigDecimal("45000"))), new BigDecimal("225000")),
                new Transaction("prev-5", LocalDateTime.of(2026, 9, 11, 18, 0), "show-5", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 5, new BigDecimal("45000"))), new BigDecimal("225000")),
                new Transaction("prev-6", LocalDateTime.of(2026, 9, 12, 20, 0), "show-6", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 10, new BigDecimal("45000"))), new BigDecimal("450000")),
                new Transaction("prev-7", LocalDateTime.of(2026, 9, 13, 20, 0), "show-7", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 10, new BigDecimal("45000"))), new BigDecimal("450000")),
                new Transaction("curr-1", LocalDateTime.of(2026, 9, 14, 18, 0), "show-8", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 2, new BigDecimal("45000"))), new BigDecimal("90000")),
                new Transaction("curr-2", LocalDateTime.of(2026, 9, 15, 18, 0), "show-9", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 2, new BigDecimal("45000"))), new BigDecimal("90000")),
                new Transaction("curr-3", LocalDateTime.of(2026, 9, 16, 18, 0), "show-10", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 2, new BigDecimal("45000"))), new BigDecimal("90000")),
                new Transaction("curr-4", LocalDateTime.of(2026, 9, 17, 18, 0), "show-11", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 2, new BigDecimal("45000"))), new BigDecimal("90000")),
                new Transaction("curr-5", LocalDateTime.of(2026, 9, 18, 18, 0), "show-12", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 2, new BigDecimal("45000"))), new BigDecimal("90000")),
                new Transaction("curr-6", LocalDateTime.of(2026, 9, 19, 20, 0), "show-13", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 7, new BigDecimal("45000"))), new BigDecimal("315000")),
                new Transaction("curr-7", LocalDateTime.of(2026, 9, 20, 20, 0), "show-14", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-drink", 7, new BigDecimal("45000"))), new BigDecimal("315000"))
        ));

        RecommendationProperties properties = new RecommendationProperties();
        properties.setSafetyFactor(0.15);
        InventoryService inventoryService = new InventoryService(productRepository, inventoryRepository, new FnbAnalyticsService(transactionRepository, productRepository));
        InventoryRecommendationStrategy strategy = new InventoryRecommendationStrategy(productRepository, transactionRepository, inventoryService, properties);

        Recommendation recommendation = strategy.recommend(weekStart, weekEnd).get(0);
        assertTrue(recommendation.getAction().startsWith("Nhập thêm "));
        assertTrue(recommendation.getReason().contains("giới hạn ở") || recommendation.getReason().contains("Độ tin cậy"));
    }

    @Test
    void shouldTreatMissingPreviousWeekAsZeroGrowth() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();
        InMemoryInventoryRepository inventoryRepository = new InMemoryInventoryRepository();
        Product product = new Product("prod-nugget", "Nugget", ProductType.SINGLE, new BigDecimal("55000"), "pack");
        productRepository.save(product);

        inventoryRepository.saveAll(List.of(
                new InventoryTransaction("open", "prod-nugget", InventoryTransactionType.IMPORT, 10, LocalDate.of(2026, 9, 7)),
                new InventoryTransaction("exp", "prod-nugget", InventoryTransactionType.EXPORT, 5, LocalDate.of(2026, 9, 15))
        ));

        LocalDate weekStart = LocalDate.of(2026, 9, 14);
        LocalDate weekEnd = LocalDate.of(2026, 9, 20);

        transactionRepository.saveAll(List.of(
                new Transaction("curr-1", LocalDateTime.of(2026, 9, 14, 18, 0), "show-1", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-nugget", 4, new BigDecimal("55000"))), new BigDecimal("220000")),
                new Transaction("curr-2", LocalDateTime.of(2026, 9, 15, 18, 0), "show-2", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-nugget", 4, new BigDecimal("55000"))), new BigDecimal("220000")),
                new Transaction("curr-3", LocalDateTime.of(2026, 9, 16, 18, 0), "show-3", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-nugget", 4, new BigDecimal("55000"))), new BigDecimal("220000")),
                new Transaction("curr-4", LocalDateTime.of(2026, 9, 17, 18, 0), "show-4", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-nugget", 4, new BigDecimal("55000"))), new BigDecimal("220000")),
                new Transaction("curr-5", LocalDateTime.of(2026, 9, 18, 18, 0), "show-5", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-nugget", 4, new BigDecimal("55000"))), new BigDecimal("220000")),
                new Transaction("curr-6", LocalDateTime.of(2026, 9, 19, 20, 0), "show-6", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-nugget", 5, new BigDecimal("55000"))), new BigDecimal("275000")),
                new Transaction("curr-7", LocalDateTime.of(2026, 9, 20, 20, 0), "show-7", "movie-1", List.of(), List.of(new Transaction.FnbItem("prod-nugget", 5, new BigDecimal("55000"))), new BigDecimal("275000"))
        ));

        RecommendationProperties properties = new RecommendationProperties();
        properties.setSafetyFactor(0.15);
        InventoryService inventoryService = new InventoryService(productRepository, inventoryRepository, new FnbAnalyticsService(transactionRepository, productRepository));
        InventoryRecommendationStrategy strategy = new InventoryRecommendationStrategy(productRepository, transactionRepository, inventoryService, properties);

        Recommendation recommendation = strategy.recommend(weekStart, weekEnd).get(0);
        assertTrue(recommendation.getReason().contains("chưa có dữ liệu tuần trước"));
        assertTrue(recommendation.getAction().startsWith("Nhập thêm "));
    }

    @Test
    void shouldSeedInventoryExportsMatchingSalesAndRecommendComboAOnly() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();
        InMemoryInventoryRepository inventoryRepository = new InMemoryInventoryRepository();

        new DataSeeder(productRepository, transactionRepository, inventoryRepository).seedData();

        FnbAnalyticsService fnbAnalyticsService = new FnbAnalyticsService(transactionRepository, productRepository);
        Map<String, Integer> soldByProduct = fnbAnalyticsService.getQuantitySoldByProduct(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));

        for (String productId : List.of("prod-combo-a", "prod-combo-b", "prod-popcorn", "prod-soft-drink", "prod-hotdog")) {
            int soldInWeek = soldByProduct.getOrDefault(productId, 0);
            int exportInWeek = inventoryRepository.findByProductId(productId).stream()
                    .filter(tx -> tx.getType() == InventoryTransactionType.EXPORT)
                    .filter(tx -> !tx.getDate().isBefore(LocalDate.of(2026, 9, 14)) && !tx.getDate().isAfter(LocalDate.of(2026, 9, 20)))
                    .mapToInt(InventoryTransaction::getQuantity)
                    .sum();
            assertEquals(soldInWeek, exportInWeek, "Inventory exports should match product sales for " + productId);
        }

        RecommendationProperties properties = new RecommendationProperties();
        InventoryService inventoryService = new InventoryService(productRepository, inventoryRepository, fnbAnalyticsService);
        InventoryRecommendationStrategy strategy = new InventoryRecommendationStrategy(productRepository, transactionRepository, inventoryService, properties);
        List<Recommendation> restocks = strategy.recommend(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));

        assertTrue(restocks.stream().anyMatch(rec -> rec.getTargetId().equals("prod-combo-a")));
        assertTrue(restocks.stream().noneMatch(rec -> rec.getTargetId().equals("prod-combo-b")));
        assertTrue(restocks.stream().noneMatch(rec -> rec.getTargetId().equals("prod-popcorn")));
        assertTrue(restocks.stream().noneMatch(rec -> rec.getTargetId().equals("prod-soft-drink")));
        assertTrue(restocks.stream().noneMatch(rec -> rec.getTargetId().equals("prod-hotdog")));
    }
}
