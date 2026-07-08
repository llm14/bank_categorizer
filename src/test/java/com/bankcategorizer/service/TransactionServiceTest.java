package com.bankcategorizer.service;

import com.bankcategorizer.dto.TransactionResponse;
import com.bankcategorizer.dto.TransactionUpdateRequest;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.model.Transaction;
import com.bankcategorizer.repository.CategoryRepository;
import com.bankcategorizer.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, categoryRepository);
    }

    @Test
    void findAll_returnsMappedResponses() {
        Category groceries = Category.builder().id(1L).name("Groceries").build();
        Transaction categorized = Transaction.builder()
                .id(1L).date(LocalDate.of(2024, 1, 1)).description("Supermarket")
                .amount(new BigDecimal("10.00")).category(groceries).build();
        Transaction uncategorized = Transaction.builder()
                .id(2L).date(LocalDate.of(2024, 1, 2)).description("Unknown merchant")
                .amount(new BigDecimal("5.00")).build();
        when(transactionRepository.findAll()).thenReturn(List.of(categorized, uncategorized));

        List<TransactionResponse> responses = transactionService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).categoryId()).isEqualTo(1L);
        assertThat(responses.get(0).categoryName()).isEqualTo("Groceries");
        assertThat(responses.get(1).categoryId()).isNull();
        assertThat(responses.get(1).categoryName()).isNull();
    }

    @Test
    void findUncategorized_delegatesToRepositoryFilter() {
        Transaction uncategorized = Transaction.builder()
                .id(2L).date(LocalDate.of(2024, 1, 2)).description("Unknown merchant")
                .amount(new BigDecimal("5.00")).build();
        when(transactionRepository.findByCategoryIsNull()).thenReturn(List.of(uncategorized));

        List<TransactionResponse> responses = transactionService.findUncategorized();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(2L);
        assertThat(responses.get(0).categoryId()).isNull();
    }

    @Test
    void updateCategory_existingTransactionAndCategory_setsCategoryAndReturnsResponse() {
        Transaction transaction = Transaction.builder()
                .id(1L).date(LocalDate.of(2024, 1, 1)).description("Mystery charge")
                .amount(new BigDecimal("20.00")).build();
        Category category = Category.builder().id(3L).name("Transport").build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.updateCategory(1L, new TransactionUpdateRequest(3L));

        assertThat(response.categoryId()).isEqualTo(3L);
        assertThat(response.categoryName()).isEqualTo("Transport");
        assertThat(transaction.getCategory()).isEqualTo(category);
        // Correcting a transaction must never mutate the category's matching keywords.
        assertThat(category.getKeywords()).isEmpty();
    }

    @Test
    void updateCategory_missingTransaction_throwsResourceNotFoundException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateCategory(99L, new TransactionUpdateRequest(3L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).findById(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateCategory_missingCategory_throwsResourceNotFoundException() {
        Transaction transaction = Transaction.builder()
                .id(1L).date(LocalDate.of(2024, 1, 1)).description("Mystery charge")
                .amount(new BigDecimal("20.00")).build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateCategory(1L, new TransactionUpdateRequest(99L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }
}
