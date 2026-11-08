package com.cinema.analytics.model;

import java.time.LocalDate;
import java.util.Objects;

public class InventoryTransaction {
    private String id;
    private String productId;
    private InventoryTransactionType type;
    private int quantity;
    private LocalDate date;

    public InventoryTransaction() {
    }

    public InventoryTransaction(String id, String productId, InventoryTransactionType type, int quantity, LocalDate date) {
        this.id = id;
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public InventoryTransactionType getType() {
        return type;
    }

    public void setType(InventoryTransactionType type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryTransaction that = (InventoryTransaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
