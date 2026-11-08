package com.cinema.analytics.dto;

import java.math.BigDecimal;

public record MovieRevenueDto(String movieId, String movieName, BigDecimal revenue) {
}
