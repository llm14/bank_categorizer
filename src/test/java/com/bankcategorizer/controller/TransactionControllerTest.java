package com.bankcategorizer.controller;

import com.bankcategorizer.config.ClockConfig;
import com.bankcategorizer.dto.PageResponse;
import com.bankcategorizer.dto.TransactionResponse;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Import(ClockConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void findAll_noFilter_returns200WithAllTransactions() throws Exception {
        Pageable defaultPageable = PageRequest.of(0, 20, Sort.by("date").descending());
        given(transactionService.findAll(eq(defaultPageable))).willReturn(new PageResponse<>(List.of(
                new TransactionResponse(1L, LocalDate.of(2024, 1, 1), "Supermarket", new BigDecimal("10.00"), 1L, "Groceries"),
                new TransactionResponse(2L, LocalDate.of(2024, 1, 2), "Unknown merchant", new BigDecimal("5.00"), null, null)
        ), 0, 20, 2, 1));

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$.content[1].categoryId").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void findAll_uncategorizedFilter_returns200WithOnlyUncategorized() throws Exception {
        Pageable defaultPageable = PageRequest.of(0, 20, Sort.by("date").descending());
        given(transactionService.findUncategorized(eq(defaultPageable))).willReturn(new PageResponse<>(List.of(
                new TransactionResponse(2L, LocalDate.of(2024, 1, 2), "Unknown merchant", new BigDecimal("5.00"), null, null)
        ), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/transactions").param("category", "uncategorized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(2));
    }

    @Test
    void findAll_explicitPageAndSize_passesPageableThroughAndReturnsMatchingPage() throws Exception {
        Pageable requestedPageable = PageRequest.of(1, 1, Sort.by("date").descending());
        given(transactionService.findAll(eq(requestedPageable))).willReturn(new PageResponse<>(List.of(
                new TransactionResponse(3L, LocalDate.of(2024, 1, 3), "Second page item", new BigDecimal("7.00"), null, null)
        ), 1, 1, 3, 3));

        mockMvc.perform(get("/api/v1/transactions").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
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

    @Test
    void updateCategory_nonNumericId_returns400WithErrorBody() throws Exception {
        mockMvc.perform(patch("/api/v1/transactions/{id}", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getById_onlyPatchMapped_returns405WithErrorBody() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/{id}", 3L))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unknownRoute_returns404WithErrorBody() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
