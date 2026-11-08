package com.cinema.analytics.repository;

import com.cinema.analytics.model.Product;
import com.cinema.analytics.model.ProductType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    List<Product> saveAll(Collection<Product> products);

    Optional<Product> findById(String id);

    List<Product> findAll();

    List<Product> findAllByType(ProductType type);

    void deleteAll();
}
