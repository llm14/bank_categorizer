package com.bankcategorizer.controller;

import com.bankcategorizer.dto.SpendingResponse;
import com.bankcategorizer.exception.InvalidDateRangeException;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.service.SpendingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpendingController.class)
class SpendingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpendingService spendingService;

    private final LocalDate from = LocalDate.of(2024, 1, 1);
    private final LocalDate to = LocalDate.of(2024, 1, 31);

    @Test
    void getSpending_withCategory_returns200WithSingleTotal() throws Exception {
        given(spendingService.getSpendingForCategory(eq(1L), eq(from), eq(to)))
                .willReturn(new SpendingResponse(1L, "Groceries", from, to, new BigDecimal("150.32")));

        mockMvc.perform(get("/api/v1/spending")
                        .param("category", "1")
                        .param("from", "2024-01-01")
                        .param("to", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Groceries"))
                .andExpect(jsonPath("$.totalSpent").value(150.32));
    }

    @Test
    void getSpending_withoutCategory_returns200WithBreakdown() throws Exception {
        given(spendingService.getSpendingBreakdown(from, to)).willReturn(List.of(
                new SpendingResponse(1L, "Groceries", from, to, new BigDecimal("150.32")),
                new SpendingResponse(2L, "Transport", from, to, new BigDecimal("40.00"))
        ));

        mockMvc.perform(get("/api/v1/spending")
                        .param("from", "2024-01-01")
                        .param("to", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$[1].categoryName").value("Transport"));
    }

    @Test
    void getSpending_categoryNotFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Category 99 not found"))
                .given(spendingService).getSpendingForCategory(eq(99L), any(), any());

        mockMvc.perform(get("/api/v1/spending")
                        .param("category", "99")
                        .param("from", "2024-01-01")
                        .param("to", "2024-01-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category 99 not found"));
    }

    @Test
    void getSpending_fromAfterTo_returns400() throws Exception {
        willThrow(new InvalidDateRangeException("'from' date must not be after 'to' date"))
                .given(spendingService).getSpendingForCategory(eq(1L), any(), any());

        mockMvc.perform(get("/api/v1/spending")
                        .param("category", "1")
                        .param("from", "2024-01-31")
                        .param("to", "2024-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getSpending_missingDates_returns400() throws Exception {
        willThrow(new InvalidDateRangeException("Both 'from' and 'to' dates are required"))
                .given(spendingService).getSpendingBreakdown(any(), any());

        mockMvc.perform(get("/api/v1/spending"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
