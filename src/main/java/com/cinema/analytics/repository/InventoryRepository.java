package com.cinema.analytics.repository;

import com.cinema.analytics.model.InventoryTransaction;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface InventoryRepository {
    InventoryTransaction save(InventoryTransaction transaction);

    List<InventoryTransaction> saveAll(Collection<InventoryTransaction> transactions);

    List<InventoryTransaction> findAll();

    List<InventoryTransaction> findByDateRange(LocalDate start, LocalDate end);

    List<InventoryTransaction> findByProductId(String productId);

    void deleteAll();
}
