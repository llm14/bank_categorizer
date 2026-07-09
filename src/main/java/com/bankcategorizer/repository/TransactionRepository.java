package com.bankcategorizer.repository;

import com.bankcategorizer.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByCategoryIsNull(Pageable pageable);

    /**
     * Nulls out the category on every transaction referencing it, so a category can be
     * deleted without cascade-deleting the transactions that reference it.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Transaction t SET t.category = NULL WHERE t.category.id = :categoryId")
    void uncategorizeByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Net signed sum of a category's transaction amounts over a date range, computed at the
     * database level. Returns {@link BigDecimal#ZERO} rather than {@code null} when no
     * transactions match.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.category.id = :categoryId AND t.date BETWEEN :from AND :to")
    BigDecimal sumAmountByCategoryAndDateBetween(@Param("categoryId") Long categoryId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Net signed sum per category over a date range, excluding uncategorized transactions
     * directly in SQL. A category with zero matching transactions simply has no row in the
     * result, thanks to the {@code GROUP BY}.
     */
    @Query("SELECT new com.bankcategorizer.repository.CategorySpendingTotal(t.category.id, t.category.name, SUM(t.amount)) "
            + "FROM Transaction t WHERE t.category IS NOT NULL AND t.date BETWEEN :from AND :to "
            + "GROUP BY t.category.id, t.category.name")
    List<CategorySpendingTotal> sumAmountByCategoryGroupedForDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
