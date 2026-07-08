package com.bankcategorizer.controller;

import com.bankcategorizer.dto.SpendingResponse;
import com.bankcategorizer.service.SpendingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoint for answering "how much did I spend?" over a date range: a single category's
 * total when {@code category} is given, or a breakdown across all categories otherwise.
 */
@RestController
@RequestMapping("/api/v1/spending")
public class SpendingController {

    private final SpendingService spendingService;

    public SpendingController(SpendingService spendingService) {
        this.spendingService = spendingService;
    }

    @GetMapping
    public ResponseEntity<?> getSpending(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (category == null) {
            List<SpendingResponse> breakdown = spendingService.getSpendingBreakdown(from, to);
            return ResponseEntity.ok(breakdown);
        }
        SpendingResponse response = spendingService.getSpendingForCategory(category, from, to);
        return ResponseEntity.ok(response);
    }
}
