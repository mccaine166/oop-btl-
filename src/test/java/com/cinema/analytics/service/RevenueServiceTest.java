package com.cinema.analytics.service;

import com.cinema.analytics.model.Ticket;
import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.repository.memory.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RevenueServiceTest {
    private RevenueService revenueService;

    @BeforeEach
    void setUp() {
        InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
        repository.saveAll(List.of(
                new Transaction(
                        "tx-1",
                        LocalDateTime.of(2026, 9, 14, 20, 30),
                        "show-1",
                        "movie-1",
                        List.of(
                                new Ticket("t-1", "A1", new BigDecimal("60000")),
                                new Ticket("t-2", "A2", new BigDecimal("60000"))
                        ),
                        List.of(
                                new Transaction.FnbItem("prod-popcorn", 2, new BigDecimal("45000")),
                                new Transaction.FnbItem("prod-combo-a", 1, new BigDecimal("129000"))
                        ),
                        new BigDecimal("318000")
                ),
                new Transaction(
                        "tx-2",
                        LocalDateTime.of(2026, 9, 15, 21, 15),
                        "show-2",
                        "movie-2",
                        List.of(
                                new Ticket("t-3", "B1", new BigDecimal("80000"))
                        ),
                        List.of(
                                new Transaction.FnbItem("prod-soft-drink", 3, new BigDecimal("28000"))
                        ),
                        new BigDecimal("164000")
                )
        ));
        revenueService = new RevenueService(repository);
    }

    @Test
    void shouldCalculateRevenueMetricsFromTransactions() {
        LocalDate start = LocalDate.of(2026, 9, 14);
        LocalDate end = LocalDate.of(2026, 9, 15);

        assertEquals(new BigDecimal("503000"), revenueService.getTotalRevenue(start, end));
        assertEquals(new BigDecimal("200000"), revenueService.getTicketRevenue(start, end));
        assertEquals(new BigDecimal("303000"), revenueService.getFoodRevenue(start, end));
        assertEquals(new BigDecimal("251500.00"), revenueService.getAverageTransactionValue(start, end));
        assertEquals(new BigDecimal("39.76"), revenueService.getTicketRevenueShare(start, end));
        assertEquals(new BigDecimal("60.24"), revenueService.getFoodRevenueShare(start, end));
        assertEquals(new BigDecimal("339000"), revenueService.getDailyRevenue(start, end).get(LocalDate.of(2026, 9, 14)));
        assertEquals(new BigDecimal("339000"), revenueService.getRevenueByMovie(start, end).get("movie-1"));
        assertEquals(new BigDecimal("164000"), revenueService.getRevenueByShowtime(start, end).get("show-2"));
        assertEquals(new BigDecimal("100.00"), revenueService.getTicketRevenueShare(start, end).add(revenueService.getFoodRevenueShare(start, end)));
    }
}
