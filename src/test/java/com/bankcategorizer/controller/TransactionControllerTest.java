package com.bankcategorizer.controller;

import com.bankcategorizer.dto.TransactionResponse;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void findAll_noFilter_returns200WithAllTransactions() throws Exception {
        given(transactionService.findAll()).willReturn(List.of(
                new TransactionResponse(1L, LocalDate.of(2024, 1, 1), "Supermarket", new BigDecimal("10.00"), 1L, "Groceries"),
                new TransactionResponse(2L, LocalDate.of(2024, 1, 2), "Unknown merchant", new BigDecimal("5.00"), null, null)
        ));

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$[1].categoryId").doesNotExist());
    }

    @Test
    void findAll_uncategorizedFilter_returns200WithOnlyUncategorized() throws Exception {
        given(transactionService.findUncategorized()).willReturn(List.of(
                new TransactionResponse(2L, LocalDate.of(2024, 1, 2), "Unknown merchant", new BigDecimal("5.00"), null, null)
        ));

        mockMvc.perform(get("/api/v1/transactions").param("category", "uncategorized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void findAll_unsupportedFilterValue_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/transactions").param("category", "groceries"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateCategory_validRequest_returns200WithUpdatedTransaction() throws Exception {
        given(transactionService.updateCategory(eq(1L), any())).willReturn(
                new TransactionResponse(1L, LocalDate.of(2024, 1, 1), "Mystery charge", new BigDecimal("20.00"), 3L, "Transport"));

        mockMvc.perform(patch("/api/v1/transactions/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryId").value(3))
                .andExpect(jsonPath("$.categoryName").value("Transport"));
    }

    @Test
    void updateCategory_missingCategoryId_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/transactions/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_missingTransaction_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Transaction 99 not found"))
                .given(transactionService).updateCategory(eq(99L), any());

        mockMvc.perform(patch("/api/v1/transactions/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":3}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Transaction 99 not found"));
    }

    @Test
    void updateCategory_missingCategory_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Category 99 not found"))
                .given(transactionService).updateCategory(eq(1L), any());

        mockMvc.perform(patch("/api/v1/transactions/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":99}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category 99 not found"));
    }
}
