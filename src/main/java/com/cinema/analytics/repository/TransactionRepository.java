package com.cinema.analytics.repository;

import com.cinema.analytics.model.Transaction;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> saveAll(Collection<Transaction> transactions);

    Optional<Transaction> findById(String id);

    List<Transaction> findAll();

    List<Transaction> findByDateRange(LocalDate start, LocalDate end);

    void deleteAll();
}
