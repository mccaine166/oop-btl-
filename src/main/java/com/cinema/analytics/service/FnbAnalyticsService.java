package com.cinema.analytics.service;

import com.cinema.analytics.model.Product;
import com.cinema.analytics.model.ProductType;
import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.repository.ProductRepository;
import com.cinema.analytics.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FnbAnalyticsService {
    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;

    public FnbAnalyticsService(TransactionRepository transactionRepository, ProductRepository productRepository) {
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
    }

    public Map<String, Integer> getQuantitySoldByProduct(LocalDate start, LocalDate end) {
        return transactionsInRange(start, end).stream()
                .flatMap(transaction -> transaction.getFoodItems().stream())
                .collect(Collectors.groupingBy(
                        Transaction.FnbItem::getProductId,
                        LinkedHashMap::new,
                        Collectors.summingInt(Transaction.FnbItem::getQuantity)
                ));
    }

    public List<ProductSalesSummary> getTopProducts(LocalDate start, LocalDate end, int limit) {
        Map<String, Integer> soldByProduct = getQuantitySoldByProduct(start, end);
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(product -> soldByProduct.containsKey(product.getId()))
                .map(product -> new ProductSalesSummary(
                        product.getId(),
                        product.getName(),
                        soldByProduct.get(product.getId()),
                        getRevenueForProduct(product.getId(), start, end)
                ))
                .sorted(Comparator.comparing(ProductSalesSummary::quantity).reversed()
                        .thenComparing(ProductSalesSummary::productName))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<ProductSalesSummary> getTopComboProducts(LocalDate start, LocalDate end, int limit) {
        return getTopProducts(start, end, Integer.MAX_VALUE).stream()
                .filter(summary -> productRepository.findById(summary.productId())
                        .map(Product::getType)
                        .orElse(ProductType.SINGLE) == ProductType.COMBO)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, BigDecimal> getRevenueByProduct(LocalDate start, LocalDate end) {
        return transactionsInRange(start, end).stream()
                .flatMap(transaction -> transaction.getFoodItems().stream())
                .collect(Collectors.groupingBy(
                        Transaction.FnbItem::getProductId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
    }

    public BigDecimal getAverageSalesPerDay(LocalDate start, LocalDate end) {
        int totalQuantity = getQuantitySoldByProduct(start, end).values().stream().mapToInt(Integer::intValue).sum();
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        if (days <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalQuantity)
                .divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAverageSalesPerDay(String productId, LocalDate start, LocalDate end) {
        int totalQuantity = getQuantitySoldByProduct(start, end).getOrDefault(productId, 0);
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        if (days <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalQuantity)
                .divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getProductRevenue(String productId, LocalDate start, LocalDate end) {
        return getRevenueByProduct(start, end).getOrDefault(productId, BigDecimal.ZERO);
    }

    public int getQuantitySoldByProduct(String productId, LocalDate start, LocalDate end) {
        return getQuantitySoldByProduct(start, end).getOrDefault(productId, 0);
    }

    public BigDecimal getConsumptionRate(String productId, LocalDate start, LocalDate end) {
        return getAverageSalesPerDay(productId, start, end);
    }

    private BigDecimal getRevenueForProduct(String productId, LocalDate start, LocalDate end) {
        return transactionsInRange(start, end).stream()
                .flatMap(transaction -> transaction.getFoodItems().stream())
                .filter(item -> item.getProductId().equals(productId))
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Transaction> transactionsInRange(LocalDate start, LocalDate end) {
        return transactionRepository.findByDateRange(start, end);
    }

    public record ProductSalesSummary(String productId, String productName, int quantity, BigDecimal revenue) {
    }
}
