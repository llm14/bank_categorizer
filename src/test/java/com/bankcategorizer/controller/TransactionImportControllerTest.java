package com.bankcategorizer.controller;

import com.bankcategorizer.dto.ImportResultResponse;
import com.bankcategorizer.exception.InvalidFileFormatException;
import com.bankcategorizer.service.TransactionImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionImportController.class)
class TransactionImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionImportService transactionImportService;

    @Test
    void importTransactions_validFile_returns200WithSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", "Date,Description,Amount\n2024-01-01,Test,10.00\n".getBytes());

        given(transactionImportService.importTransactions(any()))
                .willReturn(new ImportResultResponse("transactions.csv", 1, 1, 0));

        mockMvc.perform(multipart("/api/v1/transactions/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("transactions.csv"))
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(0));
    }

    @Test
    void importTransactions_unsupportedFileType_returns400WithErrorBody() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.txt", "text/plain", "irrelevant".getBytes());

        given(transactionImportService.importTransactions(any()))
                .willThrow(new InvalidFileFormatException("Unsupported file type for 'transactions.txt'"));

        mockMvc.perform(multipart("/api/v1/transactions/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported file type for 'transactions.txt'"));
    }
}
