package com.cinema.analytics.recommendation;

import java.time.LocalDate;

public record ShowtimeOccupancy(String movieId, LocalDate date, String timeSlot, double occupancy) {
}
