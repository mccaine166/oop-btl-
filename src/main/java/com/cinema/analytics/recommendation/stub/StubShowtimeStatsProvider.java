package com.cinema.analytics.recommendation.stub;

import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.recommendation.ShowtimeOccupancy;
import com.cinema.analytics.recommendation.ShowtimeStatsProvider;
import com.cinema.analytics.repository.TransactionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("stub")
public class StubShowtimeStatsProvider implements ShowtimeStatsProvider {
    private final TransactionRepository transactionRepository;

    public StubShowtimeStatsProvider(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<ShowtimeOccupancy> getOccupancySnapshot(LocalDate start, LocalDate end) {
        List<ShowtimeOccupancy> results = new ArrayList<>();
        for (Transaction transaction : transactionRepository.findByDateRange(start, end)) {
            int ticketCount = transaction.getTickets().size();
            double baseOccupancy = Math.min(0.96, ticketCount / 12.0d);
            double weekendBoost = isWeekend(transaction.getCreatedAt().toLocalDate()) ? 0.08 : 0.0;
            double eveningBoost = isEveningPeak(transaction.getCreatedAt().getHour()) ? 0.10 : 0.0;
            double occupancy = Math.min(0.96, baseOccupancy + weekendBoost + eveningBoost);
            String timeSlot = resolveTimeSlot(transaction.getCreatedAt().getHour());
            results.add(new ShowtimeOccupancy(transaction.getMovieId(), transaction.getCreatedAt().toLocalDate(), timeSlot, occupancy));
        }
        return results;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isEveningPeak(int hour) {
        return hour >= 19 && hour <= 22;
    }

    private String resolveTimeSlot(int hour) {
        if (hour >= 19 && hour <= 22) {
            return "19:00–22:00";
        }
        if (hour >= 14 && hour < 19) {
            return "14:00–18:59";
        }
        return "09:00–13:59";
    }
}
