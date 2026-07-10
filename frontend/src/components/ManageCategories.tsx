import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import type { FormEvent } from "react";
import { createCategory, deleteCategory, listCategories } from "../api/categories";
import { ApiError } from "../api/client";
import type { CategoryResponse } from "../api/types";

const CATEGORIES_QUERY_KEY = ["categories"];

function errorMessage(error: unknown): string | null {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return null;
}

function parseKeywords(raw: string): string[] {
  return raw
    .split(",")
    .map((keyword) => keyword.trim())
    .filter((keyword) => keyword.length > 0);
}

/**
 * Manage categories screen (FE-4): lists all categories (GET /api/v1/categories), lets the
 * user create a new one (POST /api/v1/categories) with inline validation (400) and
 * duplicate-name (409) errors, and delete an existing one (DELETE /api/v1/categories/{id})
 * after confirming - deleting a category uncategorizes its transactions rather than deleting
 * them (see CategoryService.delete), so the confirmation copy says so explicitly.
 */
export function ManageCategories() {
  const queryClient = useQueryClient();
  const categoriesQuery = useQuery({
    queryKey: CATEGORIES_QUERY_KEY,
    queryFn: listCategories,
  });

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [keywords, setKeywords] = useState("");

  const createMutation = useMutation({
    mutationFn: createCategory,
    onSuccess: () => {
      setName("");
      setDescription("");
      setKeywords("");
      void queryClient.invalidateQueries({ queryKey: CATEGORIES_QUERY_KEY });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCategory,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: CATEGORIES_QUERY_KEY });
    },
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    createMutation.mutate({
      name: name.trim(),
      description: description.trim() ? description.trim() : undefined,
      keywords: parseKeywords(keywords),
    });
  }

  function handleDelete(category: CategoryResponse) {
    const confirmed = window.confirm(
      `Delete category "${category.name}"? Its transactions will become uncategorized, not deleted.`,
    );
    if (!confirmed) {
      return;
    }
    deleteMutation.mutate(category.id);
  }

  return (
    <div className="mx-auto mt-8 max-w-2xl rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-semibold text-gray-900">Manage categories</h2>
      <p className="mt-1 text-sm text-gray-500">
        Create categories and keywords used to auto-categorize transactions.
      </p>

      <form className="mt-4 space-y-3" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="category-name" className="block text-sm font-medium text-gray-700">
            Name
          </label>
          <input
            id="category-name"
            type="text"
            required
            value={name}
            onChange={(event) => setName(event.target.value)}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
          />
        </div>

        <div>
          <label htmlFor="category-description" className="block text-sm font-medium text-gray-700">
            Description (optional)
          </label>
          <input
            id="category-description"
            type="text"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
          />
        </div>

        <div>
          <label htmlFor="category-keywords" className="block text-sm font-medium text-gray-700">
            Keywords (comma-separated, optional)
          </label>
          <input
            id="category-keywords"
            type="text"
            placeholder="e.g. netflix, spotify"
            value={keywords}
            onChange={(event) => setKeywords(event.target.value)}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
          />
        </div>

        <button
          type="submit"
          disabled={!name.trim() || createMutation.isPending}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300"
        >
          {createMutation.isPending ? "Creating..." : "Create category"}
        </button>

        {createMutation.isError && (
          <p role="alert" className="text-sm text-red-700">
            {errorMessage(createMutation.error)}
          </p>
        )}
      </form>

      <div className="mt-6">
        {categoriesQuery.isLoading && <p className="text-gray-600">Loading categories...</p>}

        {categoriesQuery.isError && (
          <p className="text-red-700">{errorMessage(categoriesQuery.error) ?? "Failed to load categories."}</p>
        )}

        {categoriesQuery.isSuccess && categoriesQuery.data.length === 0 && (
          <p className="text-gray-600">No categories yet.</p>
        )}

        {categoriesQuery.isSuccess && categoriesQuery.data.length > 0 && (
          <ul className="mt-2 divide-y divide-gray-200">
            {categoriesQuery.data.map((category) => (
              <li key={category.id} className="flex items-start justify-between gap-4 py-3">
                <div>
                  <p className="font-medium text-gray-900">{category.name}</p>
                  {category.description && <p className="text-sm text-gray-500">{category.description}</p>}
                  <p className="mt-1 text-xs text-gray-400">
                    {category.keywords.length > 0 ? category.keywords.join(", ") : "No keywords"}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => handleDelete(category)}
                  disabled={deleteMutation.isPending}
                  className="shrink-0 rounded-md border border-red-300 px-3 py-1 text-sm font-medium text-red-700 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Delete
                </button>
              </li>
            ))}
          </ul>
        )}

        {deleteMutation.isError && (
          <p role="alert" className="mt-2 text-sm text-red-700">
            {errorMessage(deleteMutation.error)}
          </p>
        )}
      </div>
    </div>
  );
}
