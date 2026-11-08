package com.cinema.analytics.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Transaction {
    private String id;
    private LocalDateTime createdAt;
    private String showtimeId;
    private String movieId;
    private List<Ticket> tickets = new ArrayList<>();
    private List<FnbItem> foodItems = new ArrayList<>();
    private BigDecimal totalAmount;

    public Transaction() {
    }

    public Transaction(String id, LocalDateTime createdAt, String showtimeId, String movieId,
                      List<Ticket> tickets, List<FnbItem> foodItems, BigDecimal totalAmount) {
        this.id = id;
        this.createdAt = createdAt;
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.tickets = tickets;
        this.foodItems = foodItems;
        this.totalAmount = totalAmount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(String showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public List<FnbItem> getFoodItems() {
        return foodItems;
    }

    public void setFoodItems(List<FnbItem> foodItems) {
        this.foodItems = foodItems;
    }

    public BigDecimal getTotalAmount() {
        return getTicketRevenue().add(getFoodRevenue());
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTicketRevenue() {
        return tickets.stream()
                .map(Ticket::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getFoodRevenue() {
        return foodItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class FnbItem {
        private String productId;
        private int quantity;
        private BigDecimal unitPrice;

        public FnbItem() {
        }

        public FnbItem(String productId, int quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }
}
