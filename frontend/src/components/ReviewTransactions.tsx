import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { listCategories } from "../api/categories";
import { ApiError } from "../api/client";
import { listTransactions, updateTransactionCategory } from "../api/transactions";
import { UNCATEGORIZED_FILTER } from "../api/types";
import type { TransactionCategoryFilter, TransactionResponse } from "../api/types";

const CATEGORIES_QUERY_KEY = ["categories"];

type SortField = "date" | "amount";
type SortDirection = "asc" | "desc";

interface SortState {
  field: SortField;
  direction: SortDirection;
}

function errorMessage(error: unknown): string | null {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return null;
}

function sortParam(sort: SortState): string {
  return `${sort.field},${sort.direction}`;
}

/**
 * Review transactions screen (FE-5): a paginated, sortable table of transactions
 * (GET /api/v1/transactions), with a toggle to narrow to uncategorized-only
 * (?category=uncategorized) and a per-row category picker that corrects a transaction's
 * category via PATCH /api/v1/transactions/{id}. The list refetches via TanStack Query
 * invalidation after a successful update, no full page reload.
 */
export function ReviewTransactions() {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [uncategorizedOnly, setUncategorizedOnly] = useState(false);
  const [sort, setSort] = useState<SortState>({ field: "date", direction: "desc" });

  const transactionsParams: {
    category?: TransactionCategoryFilter;
    page: number;
    sort: string;
  } = {
    category: uncategorizedOnly ? UNCATEGORIZED_FILTER : undefined,
    page,
    sort: sortParam(sort),
  };

  const transactionsQueryKey = ["transactions", transactionsParams];

  const transactionsQuery = useQuery({
    queryKey: transactionsQueryKey,
    queryFn: () => listTransactions(transactionsParams),
  });

  const categoriesQuery = useQuery({
    queryKey: CATEGORIES_QUERY_KEY,
    queryFn: listCategories,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, categoryId }: { id: number; categoryId: number }) =>
      updateTransactionCategory(id, { categoryId }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["transactions"] });
    },
  });

  function handleFilterToggle() {
    setUncategorizedOnly((current) => !current);
    setPage(0);
  }

  function handleSort(field: SortField) {
    setSort((current) => {
      if (current.field === field) {
        return { field, direction: current.direction === "asc" ? "desc" : "asc" };
      }
      return { field, direction: "asc" };
    });
    setPage(0);
  }

  function handleCategoryChange(transaction: TransactionResponse, categoryId: string) {
    if (!categoryId) {
      return;
    }
    updateMutation.mutate({ id: transaction.id, categoryId: Number(categoryId) });
  }

  function sortIndicator(field: SortField): string {
    if (sort.field !== field) {
      return "";
    }
    return sort.direction === "asc" ? " ↑" : " ↓";
  }

  const pageData = transactionsQuery.data;
  const categories = categoriesQuery.data ?? [];

  return (
    <div className="mx-auto mt-8 max-w-4xl rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-semibold text-gray-900">Review transactions</h2>
      <p className="mt-1 text-sm text-gray-500">
        Review imported transactions and fix their category assignment.
      </p>

      <label className="mt-4 flex items-center gap-2 text-sm text-gray-700">
        <input
          type="checkbox"
          checked={uncategorizedOnly}
          onChange={handleFilterToggle}
        />
        Show uncategorized only
      </label>

      <div className="mt-4">
        {transactionsQuery.isLoading && <p className="text-gray-600">Loading transactions...</p>}

        {transactionsQuery.isError && (
          <p className="text-red-700">
            {errorMessage(transactionsQuery.error) ?? "Failed to load transactions."}
          </p>
        )}

        {transactionsQuery.isSuccess && pageData !== undefined && pageData.content.length === 0 && (
          <p className="text-gray-600">No transactions found.</p>
        )}

        {transactionsQuery.isSuccess && pageData !== undefined && pageData.content.length > 0 && (
          <>
            <table className="mt-2 w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-gray-500">
                  <th className="py-2 pr-4">
                    <button
                      type="button"
                      onClick={() => handleSort("date")}
                      className="font-medium hover:text-gray-900"
                    >
                      Date{sortIndicator("date")}
                    </button>
                  </th>
                  <th className="py-2 pr-4 font-medium">Description</th>
                  <th className="py-2 pr-4">
                    <button
                      type="button"
                      onClick={() => handleSort("amount")}
                      className="font-medium hover:text-gray-900"
                    >
                      Amount{sortIndicator("amount")}
                    </button>
                  </th>
                  <th className="py-2 pr-4 font-medium">Category</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((transaction) => (
                  <tr key={transaction.id} className="border-b border-gray-100">
                    <td className="py-2 pr-4">{transaction.date}</td>
                    <td className="py-2 pr-4">{transaction.description}</td>
                    <td className="py-2 pr-4">{transaction.amount}</td>
                    <td className="py-2 pr-4">
                      <select
                        aria-label={`Category for ${transaction.description}`}
                        value={transaction.categoryId ?? ""}
                        onChange={(event) => handleCategoryChange(transaction, event.target.value)}
                        disabled={updateMutation.isPending}
                        className="rounded-md border border-gray-300 px-2 py-1 text-sm"
                      >
                        <option value="" disabled>
                          Uncategorized
                        </option>
                        {categories.map((category) => (
                          <option key={category.id} value={category.id}>
                            {category.name}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
              <button
                type="button"
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                disabled={pageData.page <= 0}
                className="rounded-md border border-gray-300 px-3 py-1 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Previous
              </button>
              <span>
                Page {pageData.page + 1} of {Math.max(pageData.totalPages, 1)}
              </span>
              <button
                type="button"
                onClick={() => setPage((current) => current + 1)}
                disabled={pageData.page + 1 >= pageData.totalPages}
                className="rounded-md border border-gray-300 px-3 py-1 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </>
        )}

        {updateMutation.isError && (
          <p role="alert" className="mt-2 text-sm text-red-700">
            {errorMessage(updateMutation.error)}
          </p>
        )}
      </div>
    </div>
  );
}
