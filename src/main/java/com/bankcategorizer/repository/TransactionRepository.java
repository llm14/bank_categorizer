package com.bankcategorizer.repository;

import com.bankcategorizer.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCategoryIdAndDateBetween(Long categoryId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Transaction> findByCategoryIsNull();
}
