package com.cinema.analytics.service;

import com.cinema.analytics.model.Product;
import com.cinema.analytics.model.ProductType;
import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.repository.memory.InMemoryProductRepository;
import com.cinema.analytics.repository.memory.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FnbAnalyticsServiceTest {
    private FnbAnalyticsService fnbAnalyticsService;

    @BeforeEach
    void setUp() {
        InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();

        productRepository.saveAll(List.of(
                new Product("prod-combo-a", "Combo A", ProductType.COMBO, new BigDecimal("129000"), "combo"),
                new Product("prod-combo-b", "Combo B", ProductType.COMBO, new BigDecimal("99000"), "combo"),
                new Product("prod-popcorn", "Popcorn", ProductType.SINGLE, new BigDecimal("45000"), "pack"),
                new Product("prod-soft-drink", "Soft Drink", ProductType.SINGLE, new BigDecimal("28000"), "bottle")
        ));

        transactionRepository.saveAll(List.of(
                new Transaction(
                        "tx-1",
                        LocalDateTime.of(2026, 9, 14, 19, 30),
                        "show-1",
                        "movie-1",
                        List.of(),
                        List.of(
                                new Transaction.FnbItem("prod-combo-a", 2, new BigDecimal("129000")),
                                new Transaction.FnbItem("prod-popcorn", 3, new BigDecimal("45000")),
                                new Transaction.FnbItem("prod-soft-drink", 1, new BigDecimal("28000"))
                        ),
                        new BigDecimal("420000")
                ),
                new Transaction(
                        "tx-2",
                        LocalDateTime.of(2026, 9, 15, 20, 00),
                        "show-2",
                        "movie-2",
                        List.of(),
                        List.of(
                                new Transaction.FnbItem("prod-combo-a", 1, new BigDecimal("129000")),
                                new Transaction.FnbItem("prod-combo-b", 2, new BigDecimal("99000")),
                                new Transaction.FnbItem("prod-popcorn", 2, new BigDecimal("45000"))
                        ),
                        new BigDecimal("441000")
                )
        ));

        fnbAnalyticsService = new FnbAnalyticsService(transactionRepository, productRepository);
    }

    @Test
    void shouldComputeQuantityAndTopProducts() {
        LocalDate start = LocalDate.of(2026, 9, 14);
        LocalDate end = LocalDate.of(2026, 9, 15);

        assertEquals(3, fnbAnalyticsService.getQuantitySoldByProduct("prod-combo-a", start, end));
        assertEquals(5, fnbAnalyticsService.getQuantitySoldByProduct("prod-popcorn", start, end));
        assertEquals(new BigDecimal("5.50"), fnbAnalyticsService.getAverageSalesPerDay(start, end));

        List<FnbAnalyticsService.ProductSalesSummary> topProducts = fnbAnalyticsService.getTopProducts(start, end, 3);
        assertEquals("prod-popcorn", topProducts.get(0).productId());
        assertTrue(topProducts.stream().allMatch(summary -> summary.quantity() > 0));

        List<FnbAnalyticsService.ProductSalesSummary> topCombos = fnbAnalyticsService.getTopComboProducts(start, end, 10);
        assertEquals(2, topCombos.size());
        assertTrue(topCombos.stream().allMatch(summary -> summary.productId().contains("combo")));
    }
}
