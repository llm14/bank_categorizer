package com.bankcategorizer.repository;

import com.bankcategorizer.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCategoryIdAndDateBetween(Long categoryId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Transaction> findByCategoryIsNull();

    /**
     * Nulls out the category on every transaction referencing it, so a category can be
     * deleted without cascade-deleting the transactions that reference it.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Transaction t SET t.category = NULL WHERE t.category.id = :categoryId")
    void uncategorizeByCategoryId(@Param("categoryId") Long categoryId);
}
