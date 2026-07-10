package com.bankcategorizer.repository;

import com.bankcategorizer.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    /**
     * Categories in creation order, used for auto-categorization so that when more than
     * one category could match a transaction, the earliest-created one wins.
     */
    List<Category> findAllByOrderByIdAsc();
}
