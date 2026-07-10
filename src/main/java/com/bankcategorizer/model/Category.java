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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_id_seq")
    @SequenceGenerator(name = "categories_id_seq", sequenceName = "categories_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 255)
    private String description;

    // Loaded eagerly, via a single SUBSELECT rather than one query per owning row: every real
    // caller (CategoryService, CategorizationService via TransactionImportService) now runs
    // inside a transaction, so eager loading is safe, and @Fetch(SUBSELECT) collapses what
    // would otherwise be an N+1 (one keywords query per category) into a single extra query
    // for the whole batch, regardless of how many categories are loaded.
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "category_keywords", joinColumns = @JoinColumn(name = "category_id"))
    @Column(name = "keyword", nullable = false, length = 255)
    @Builder.Default
    private Set<String> keywords = new LinkedHashSet<>();
}
