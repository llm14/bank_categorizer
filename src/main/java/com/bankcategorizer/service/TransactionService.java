package com.bankcategorizer.service;

import com.bankcategorizer.dto.TransactionResponse;
import com.bankcategorizer.dto.TransactionUpdateRequest;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.model.Transaction;
import com.bankcategorizer.repository.CategoryRepository;
import com.bankcategorizer.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for listing transactions and manually assigning or correcting a
 * transaction's category. Manual (re)categorization only updates the transaction itself;
 * it never touches a category's keywords, so it does not retroactively affect the
 * auto-categorization rules from US-3.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll() {
        return transactionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findUncategorized() {
        return transactionRepository.findByCategoryIsNull().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TransactionResponse updateCategory(Long transactionId, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction %d not found".formatted(transactionId)));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category %d not found".formatted(request.categoryId())));

        transaction.setCategory(category);
        Transaction saved = transactionRepository.save(transaction);

        log.info("Transaction {} manually recategorized to category {} ('{}')",
                transactionId, category.getId(), category.getName());
        return toResponse(saved);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        Category category = transaction.getCategory();
        Long categoryId = category != null ? category.getId() : null;
        String categoryName = category != null ? category.getName() : null;
        return new TransactionResponse(transaction.getId(), transaction.getDate(), transaction.getDescription(),
                transaction.getAmount(), categoryId, categoryName);
    }
}
