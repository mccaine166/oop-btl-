package com.cinema.analytics.recommendation;

import java.time.LocalDate;
import java.util.List;

public interface ShowtimeStatsProvider {
    List<ShowtimeOccupancy> getOccupancySnapshot(LocalDate start, LocalDate end);
}
