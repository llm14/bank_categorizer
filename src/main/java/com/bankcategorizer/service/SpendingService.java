package com.bankcategorizer.service;

import com.bankcategorizer.dto.SpendingResponse;
import com.bankcategorizer.exception.InvalidDateRangeException;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.model.Transaction;
import com.bankcategorizer.repository.CategoryRepository;
import com.bankcategorizer.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for answering "how much did I spend?" questions: a single category's
 * total over a date range, or a breakdown across all categories for that range.
 *
 * <p>Transaction amounts are signed like a bank statement (expenses negative, income
 * positive). "Total spent" sums the raw signed amounts first and only then takes the
 * absolute value, so any refund netted into a spending category is accounted for rather
 * than inflating the total.
 */
@Service
public class SpendingService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public SpendingService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public SpendingResponse getSpendingForCategory(Long categoryId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category %d not found".formatted(categoryId)));

        List<Transaction> transactions = transactionRepository.findByCategoryIdAndDateBetween(categoryId, from, to);
        BigDecimal totalSpent = sumAmounts(transactions).abs();
        return new SpendingResponse(category.getId(), category.getName(), from, to, totalSpent);
    }

    @Transactional(readOnly = true)
    public List<SpendingResponse> getSpendingBreakdown(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<Transaction> transactions = transactionRepository.findByDateBetween(from, to);

        Map<Long, List<Transaction>> byCategoryId = transactions.stream()
                .filter(transaction -> transaction.getCategory() != null)
                .collect(Collectors.groupingBy(transaction -> transaction.getCategory().getId(), LinkedHashMap::new, Collectors.toList()));

        return byCategoryId.values().stream()
                .map(categoryTransactions -> {
                    Category category = categoryTransactions.get(0).getCategory();
                    BigDecimal totalSpent = sumAmounts(categoryTransactions).abs();
                    return new SpendingResponse(category.getId(), category.getName(), from, to, totalSpent);
                })
                .toList();
    }

    private BigDecimal sumAmounts(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidDateRangeException("Both 'from' and 'to' dates are required");
        }
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException("'from' date must not be after 'to' date");
        }
    }
}
