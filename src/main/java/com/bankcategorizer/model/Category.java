package com.bankcategorizer.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A spending category used to classify transactions (e.g. Groceries, Rent, Utilities).
 *
 * <p>A category can carry one or more keywords: a transaction is auto-assigned to a
 * category if any of its keywords appears as a case-insensitive substring of the
 * transaction's description (see {@code CategorizationService}).
 */
@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 255)
    private String description;

    // Loaded eagerly: matching runs outside a transactional context (during file import),
    // and the keyword set is expected to stay small, so eager loading keeps things simple.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "category_keywords", joinColumns = @JoinColumn(name = "category_id"))
    @Column(name = "keyword", nullable = false, length = 255)
    @Builder.Default
    private Set<String> keywords = new LinkedHashSet<>();
}
