package com.bankcategorizer.controller;

import com.bankcategorizer.dto.ImportResultResponse;
import com.bankcategorizer.service.TransactionImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint for uploading a bank transaction export (CSV or XLSX) for parsing and
 * persistence. Imported transactions are stored without a category; categorization
 * happens in a later step.
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionImportController {

    private final TransactionImportService transactionImportService;

    public TransactionImportController(TransactionImportService transactionImportService) {
        this.transactionImportService = transactionImportService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultResponse> importTransactions(@RequestParam("file") MultipartFile file) {
        ImportResultResponse result = transactionImportService.importTransactions(file);
        return ResponseEntity.ok(result);
    }
}
