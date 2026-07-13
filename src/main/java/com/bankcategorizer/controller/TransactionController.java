package com.bankcategorizer.controller;

import com.bankcategorizer.dto.PageResponse;
import com.bankcategorizer.dto.TransactionCreateRequest;
import com.bankcategorizer.dto.TransactionResponse;
import com.bankcategorizer.dto.TransactionUpdateRequest;
import com.bankcategorizer.exception.InvalidTransactionFilterException;
import com.bankcategorizer.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for listing transactions (optionally filtered to uncategorized ones), manually
 * adding a single transaction, and manually assigning or correcting a transaction's category.
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private static final String UNCATEGORIZED_FILTER = "uncategorized";

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> findAll(
            @RequestParam(name = "category", required = false) String category,
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        if (category == null) {
            return ResponseEntity.ok(transactionService.findAll(pageable));
        }
        if (UNCATEGORIZED_FILTER.equalsIgnoreCase(category)) {
            return ResponseEntity.ok(transactionService.findUncategorized(pageable));
        }
        throw new InvalidTransactionFilterException(
                "Unsupported category filter '%s'; only '%s' is supported".formatted(category, UNCATEGORIZED_FILTER));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionCreateRequest request) {
        TransactionResponse response = transactionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateCategory(@PathVariable Long id,
            @Valid @RequestBody TransactionUpdateRequest request) {
        return ResponseEntity.ok(transactionService.updateCategory(id, request));
    }
}
