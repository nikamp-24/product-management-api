package com.pooja.productmanagement.repository;

import com.pooja.productmanagement.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for Item entity operations.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

}
