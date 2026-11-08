package com.cinema.analytics.repository.memory;

import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.repository.TransactionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Profile("memory")
public class InMemoryTransactionRepository implements TransactionRepository {
    private final Map<String, Transaction> store = new LinkedHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        store.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public List<Transaction> saveAll(java.util.Collection<Transaction> transactions) {
        transactions.forEach(this::save);
        return findAll();
    }

    @Override
    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Transaction> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByDateRange(LocalDate start, LocalDate end) {
        return store.values().stream()
                .filter(t -> !t.getCreatedAt().toLocalDate().isBefore(start)
                        && !t.getCreatedAt().toLocalDate().isAfter(end))
                .sorted(Comparator.comparing(Transaction::getCreatedAt))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
