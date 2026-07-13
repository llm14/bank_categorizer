package com.bankcategorizer.controller;

import com.bankcategorizer.dto.SpendingBreakdownResponse;
import com.bankcategorizer.dto.SpendingComparisonResponse;
import com.bankcategorizer.dto.SpendingResponse;
import com.bankcategorizer.service.SpendingComparisonService;
import com.bankcategorizer.service.SpendingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoint for answering "how much did I spend?" over a date range: a single category's
 * total when {@code category} is given, or a breakdown across all categories (plus its
 * grand total) otherwise. {@code /compare} follows the same rule: comparing a single
 * category's periods when {@code category} is given, or all categories combined when
 * it's omitted.
 */
@RestController
@RequestMapping("/api/v1/spending")
public class SpendingController {

    private final SpendingService spendingService;
    private final SpendingComparisonService spendingComparisonService;

    public SpendingController(SpendingService spendingService, SpendingComparisonService spendingComparisonService) {
        this.spendingService = spendingService;
        this.spendingComparisonService = spendingComparisonService;
    }

    @GetMapping(params = "category")
    public ResponseEntity<SpendingResponse> getSpendingForCategory(
            @RequestParam Long category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        SpendingResponse response = spendingService.getSpendingForCategory(category, from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping(params = "!category")
    public ResponseEntity<SpendingBreakdownResponse> getSpendingBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        SpendingBreakdownResponse breakdown = spendingService.getSpendingBreakdown(from, to);
        return ResponseEntity.ok(breakdown);
    }

    @GetMapping("/compare")
    public ResponseEntity<SpendingComparisonResponse> compareSpending(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer lookback) {
        SpendingComparisonResponse response = spendingComparisonService.compare(category, period, lookback);
        return ResponseEntity.ok(response);
    }
}
