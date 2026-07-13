import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { listCategories } from "../api/categories";
import { ApiError } from "../api/client";
import { compareSpending } from "../api/spending";

const CATEGORIES_QUERY_KEY = ["categories"];

const NO_CATEGORY_VALUE = "";
const ALL_CATEGORIES_VALUE = "all";

const DEFAULT_LOOKBACK = 3;
const MIN_LOOKBACK = 1;
const MAX_LOOKBACK = 24;

function errorMessage(error: unknown): string | null {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return null;
}

/**
 * Period-over-period spending comparison screen (FE-7): pick a category and a lookback (in
 * months), then compare its current calendar month spend against its previous `lookback`
 * months and their average (GET /api/v1/spending/compare). Only `period=month` exists on the
 * backend today, so it's passed implicitly rather than exposed as a selector.
 */
export function SpendingComparison() {
  const [categoryId, setCategoryId] = useState<string>(NO_CATEGORY_VALUE);
  const [lookback, setLookback] = useState<number>(DEFAULT_LOOKBACK);

  const hasSelection = categoryId !== NO_CATEGORY_VALUE;
  const isAllCategories = categoryId === ALL_CATEGORIES_VALUE;

  const categoriesQuery = useQuery({
    queryKey: CATEGORIES_QUERY_KEY,
    queryFn: listCategories,
  });

  const comparisonQuery = useQuery({
    queryKey: ["spending", "compare", { categoryId, lookback }],
    queryFn: () =>
      compareSpending({
        category: isAllCategories ? undefined : Number(categoryId),
        period: "month",
        lookback,
      }),
    enabled: hasSelection,
  });

  const categories = categoriesQuery.data ?? [];

  return (
    <div className="mx-auto mt-8 max-w-4xl rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-semibold text-gray-900">Spending comparison</h2>
      <p className="mt-1 text-sm text-gray-500">
        Compare a category&apos;s spending this month against its previous months.
      </p>

      <div className="mt-4 flex flex-wrap items-end gap-4">
        <label className="flex flex-col text-sm text-gray-700">
          Category
          <select
            aria-label="Category"
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value)}
            className="mt-1 rounded-md border border-gray-300 px-2 py-1 text-sm"
          >
            <option value={NO_CATEGORY_VALUE}>Select a category&hellip;</option>
            <option value={ALL_CATEGORIES_VALUE}>All categories</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-col text-sm text-gray-700">
          Lookback (months)
          <input
            aria-label="Lookback (months)"
            type="number"
            min={MIN_LOOKBACK}
            max={MAX_LOOKBACK}
            value={lookback}
            onChange={(event) => setLookback(Number(event.target.value))}
            className="mt-1 w-24 rounded-md border border-gray-300 px-2 py-1 text-sm"
          />
        </label>
      </div>

      <div className="mt-4">
        {!hasSelection && (
          <p className="text-gray-600">Pick a category to compare its spending.</p>
        )}

        {hasSelection && comparisonQuery.isLoading && (
          <p className="text-gray-600">Loading comparison...</p>
        )}

        {hasSelection && comparisonQuery.isError && (
          <p className="text-red-700">
            {errorMessage(comparisonQuery.error) ?? "Failed to load spending comparison."}
          </p>
        )}

        {hasSelection && comparisonQuery.isSuccess && (
          <div className="mt-2">
            <h3 className="text-sm font-medium text-gray-500">
              {comparisonQuery.data.categoryName ?? "All categories"}
            </h3>

            <div className="mt-2 rounded-md border border-blue-200 bg-blue-50 px-3 py-2 text-sm">
              <span className="font-medium">{comparisonQuery.data.current.label}</span>{" "}
              (current): {comparisonQuery.data.current.totalSpent}
            </div>

            <table className="mt-3 w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-gray-500">
                  <th className="py-2 pr-4 font-medium">Period</th>
                  <th className="py-2 pr-4 font-medium">Total spent</th>
                </tr>
              </thead>
              <tbody>
                {comparisonQuery.data.previousPeriods.map((entry) => (
                  <tr key={entry.label} className="border-b border-gray-100">
                    <td className="py-2 pr-4">{entry.label}</td>
                    <td className="py-2 pr-4">{entry.totalSpent}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="mt-3 text-sm text-gray-900">
              <span className="font-medium">Previous average:</span>{" "}
              {comparisonQuery.data.previousAverage}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
