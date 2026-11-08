package com.cinema.analytics.service;

import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RevenueService {
    private final TransactionRepository transactionRepository;

    public RevenueService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public BigDecimal getTotalRevenue(LocalDate start, LocalDate end) {
        return transactionsInRange(start, end).stream()
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTicketRevenue(LocalDate start, LocalDate end) {
        return transactionsInRange(start, end).stream()
                .map(Transaction::getTicketRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getFoodRevenue(LocalDate start, LocalDate end) {
        return transactionsInRange(start, end).stream()
                .map(Transaction::getFoodRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<LocalDate, BigDecimal> getDailyRevenue(LocalDate start, LocalDate end) {
        return transactionsInRange(start, end).stream()
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCreatedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.mapping(Transaction::getTotalAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
    }

    public Map<String, BigDecimal> getRevenueByMovie(LocalDate start, LocalDate end) {
        return transactionsInRange(start, end).stream()
                .collect(Collectors.groupingBy(
                        Transaction::getMovieId,
                        LinkedHashMap::new,
                        Collectors.mapping(Transaction::getTotalAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
    }

    public Map<String, BigDecimal> getRevenueByShowtime(LocalDate start, LocalDate end) {
        return transactionsInRange(start, end).stream()
                .collect(Collectors.groupingBy(
                        Transaction::getShowtimeId,
                        LinkedHashMap::new,
                        Collectors.mapping(Transaction::getTotalAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
    }

    public BigDecimal getAverageTransactionValue(LocalDate start, LocalDate end) {
        List<Transaction> transactions = transactionsInRange(start, end);
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return getTotalRevenue(start, end)
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTicketRevenueShare(LocalDate start, LocalDate end) {
        BigDecimal total = getTotalRevenue(start, end);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getTicketRevenue(start, end)
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getFoodRevenueShare(LocalDate start, LocalDate end) {
        BigDecimal total = getTotalRevenue(start, end);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getFoodRevenue(start, end)
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private List<Transaction> transactionsInRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end dates are required");
        }
        return transactionRepository.findByDateRange(start, end);
    }
}
