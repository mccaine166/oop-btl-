package com.cinema.analytics.service;

import com.cinema.analytics.model.InventoryTransaction;
import com.cinema.analytics.model.InventoryTransactionType;
import com.cinema.analytics.model.Product;
import com.cinema.analytics.model.ProductType;
import com.cinema.analytics.repository.memory.InMemoryInventoryRepository;
import com.cinema.analytics.repository.memory.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryServiceTest {
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryInventoryRepository inventoryRepository = new InMemoryInventoryRepository();
        FnbAnalyticsService fnbAnalyticsService = new FnbAnalyticsService(
                new com.cinema.analytics.repository.memory.InMemoryTransactionRepository(),
                productRepository
        );

        Product product = new Product("prod-combo-a", "Combo A", ProductType.COMBO, new BigDecimal("129000"), "combo");
        productRepository.save(product);

        inventoryRepository.saveAll(List.of(
                new InventoryTransaction("inv-1", "prod-combo-a", InventoryTransactionType.IMPORT, 140, LocalDate.of(2026, 9, 14)),
                new InventoryTransaction("inv-2", "prod-combo-a", InventoryTransactionType.EXPORT, 40, LocalDate.of(2026, 9, 15)),
                new InventoryTransaction("inv-3", "prod-combo-a", InventoryTransactionType.EXPORT, 20, LocalDate.of(2026, 9, 16))
        ));

        inventoryService = new InventoryService(productRepository, inventoryRepository, fnbAnalyticsService);
    }

    @Test
    void shouldCalculateInventoryMetrics() {
        LocalDate start = LocalDate.of(2026, 9, 14);
        LocalDate end = LocalDate.of(2026, 9, 16);

        assertEquals(80, inventoryService.getCurrentStock("prod-combo-a", end));
        assertEquals(80, inventoryService.getNetInventoryMovement("prod-combo-a", start, end));
        assertNull(inventoryService.getDaysOfStockRemaining("prod-combo-a", start, end));
    }
}
