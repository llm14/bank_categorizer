package com.bankcategorizer.controller;

import com.bankcategorizer.dto.CategoryResponse;
import com.bankcategorizer.exception.DuplicateCategoryException;
import com.bankcategorizer.exception.ResourceNotFoundException;
import com.bankcategorizer.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void create_validRequest_returns201WithBody() throws Exception {
        given(categoryService.create(any())).willReturn(
                new CategoryResponse(1L, "Groceries", "Supermarket spending", List.of("supermarket", "grocery")));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Groceries","description":"Supermarket spending","keywords":["supermarket","grocery"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.description").value("Supermarket spending"))
                .andExpect(jsonPath("$.keywords.length()").value(2))
                .andExpect(jsonPath("$.keywords[0]").value("supermarket"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","description":"whatever"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        willThrow(new DuplicateCategoryException("A category named 'Groceries' already exists"))
                .given(categoryService).create(any());

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Groceries"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A category named 'Groceries' already exists"));
    }

    @Test
    void findAll_returns200WithList() throws Exception {
        given(categoryService.findAll()).willReturn(List.of(
                new CategoryResponse(1L, "Groceries", null, List.of("supermarket")),
                new CategoryResponse(2L, "Rent", "Monthly rent", List.of())
        ));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Groceries"))
                .andExpect(jsonPath("$[1].name").value("Rent"));
    }

    @Test
    void delete_existingCategory_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", 5L))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(eq(5L));
    }

    @Test
    void delete_missingCategory_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Category 99 not found"))
                .given(categoryService).delete(99L);

        mockMvc.perform(delete("/api/v1/categories/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category 99 not found"));
    }
}
