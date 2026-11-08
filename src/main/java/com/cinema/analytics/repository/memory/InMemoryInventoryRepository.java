package com.cinema.analytics.repository.memory;

import com.cinema.analytics.model.InventoryTransaction;
import com.cinema.analytics.repository.InventoryRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Profile("memory")
public class InMemoryInventoryRepository implements InventoryRepository {
    private final Map<String, InventoryTransaction> store = new LinkedHashMap<>();

    @Override
    public InventoryTransaction save(InventoryTransaction transaction) {
        store.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public List<InventoryTransaction> saveAll(java.util.Collection<InventoryTransaction> transactions) {
        transactions.forEach(this::save);
        return findAll();
    }

    @Override
    public List<InventoryTransaction> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(InventoryTransaction::getDate).thenComparing(InventoryTransaction::getProductId))
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryTransaction> findByDateRange(LocalDate start, LocalDate end) {
        return store.values().stream()
                .filter(tx -> !tx.getDate().isBefore(start) && !tx.getDate().isAfter(end))
                .sorted(Comparator.comparing(InventoryTransaction::getDate).thenComparing(InventoryTransaction::getProductId))
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryTransaction> findByProductId(String productId) {
        return store.values().stream()
                .filter(tx -> tx.getProductId().equals(productId))
                .sorted(Comparator.comparing(InventoryTransaction::getDate))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
