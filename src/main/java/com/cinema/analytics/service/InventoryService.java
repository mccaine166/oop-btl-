package com.cinema.analytics.service;

import com.cinema.analytics.model.InventoryTransaction;
import com.cinema.analytics.model.Product;
import com.cinema.analytics.repository.InventoryRepository;
import com.cinema.analytics.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class InventoryService {
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final FnbAnalyticsService fnbAnalyticsService;

    public InventoryService(ProductRepository productRepository,
                            InventoryRepository inventoryRepository,
                            FnbAnalyticsService fnbAnalyticsService) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.fnbAnalyticsService = fnbAnalyticsService;
    }

    public int getCurrentStock(String productId, LocalDate asOf) {
        return Math.max(0, inventoryRepository.findByProductId(productId).stream()
                .filter(transaction -> !transaction.getDate().isAfter(asOf))
                .mapToInt(transaction -> transaction.getType() == com.cinema.analytics.model.InventoryTransactionType.IMPORT
                        ? transaction.getQuantity()
                        : -transaction.getQuantity())
                .sum());
    }

    public int getNetInventoryMovement(String productId, LocalDate start, LocalDate end) {
        return inventoryRepository.findByProductId(productId).stream()
                .filter(transaction -> !transaction.getDate().isBefore(start) && !transaction.getDate().isAfter(end))
                .mapToInt(transaction -> transaction.getType() == com.cinema.analytics.model.InventoryTransactionType.IMPORT
                        ? transaction.getQuantity()
                        : -transaction.getQuantity())
                .sum();
    }

    public BigDecimal getAverageSalesPerDay(String productId, LocalDate start, LocalDate end) {
        return fnbAnalyticsService.getAverageSalesPerDay(productId, start, end);
    }

    public BigDecimal getConsumptionRate(String productId, LocalDate start, LocalDate end) {
        return fnbAnalyticsService.getConsumptionRate(productId, start, end);
    }

    public BigDecimal getDaysOfStockRemaining(String productId, LocalDate start, LocalDate end) {
        BigDecimal currentStock = BigDecimal.valueOf(getCurrentStock(productId, end));
        BigDecimal consumptionRate = getConsumptionRate(productId, start, end);
        if (consumptionRate.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return currentStock.divide(consumptionRate, 2, RoundingMode.HALF_UP);
    }

    public List<InventoryTransaction> getInventoryTransactions(String productId, LocalDate start, LocalDate end) {
        return inventoryRepository.findByProductId(productId).stream()
                .filter(transaction -> !transaction.getDate().isBefore(start) && !transaction.getDate().isAfter(end))
                .toList();
    }
}
