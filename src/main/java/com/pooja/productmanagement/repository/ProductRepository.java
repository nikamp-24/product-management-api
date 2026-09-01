package com.pooja.productmanagement.repository;

import com.pooja.productmanagement.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByProductName(String productName);

}
