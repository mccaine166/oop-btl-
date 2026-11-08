package com.cinema.analytics.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"memory", "stub"})
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnRevenueSummaryForValidRange() throws Exception {
        mockMvc.perform(get("/api/analytics/revenue")
                        .param("weekStart", "2026-09-14")
                        .param("weekEnd", "2026-09-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.ticketRevenue").exists())
                .andExpect(jsonPath("$.foodRevenue").exists());
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/recommendations/showtimes")
                        .param("weekStart", "2026-09-20")
                        .param("weekEnd", "2026-09-14"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenMissingWeekStart() throws Exception {
        mockMvc.perform(get("/api/analytics/revenue")
                        .param("weekEnd", "2026-09-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter: weekStart"));
    }

    @Test
    void shouldReturnBadRequestWhenMissingWeekEnd() throws Exception {
        mockMvc.perform(get("/api/analytics/revenue")
                        .param("weekStart", "2026-09-14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter: weekEnd"));
    }
}
