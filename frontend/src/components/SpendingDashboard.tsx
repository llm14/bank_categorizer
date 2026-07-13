import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { listCategories } from "../api/categories";
import { ApiError } from "../api/client";
import { getSpendingBreakdown, getSpendingForCategory } from "../api/spending";

const CATEGORIES_QUERY_KEY = ["categories"];

const ALL_CATEGORIES_VALUE = "";

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
 * Spending dashboard (FE-6): a date range picker drives either a breakdown of total spent
 * per category (GET /api/v1/spending, no category) or, when a category is selected, that
 * single category's total (GET /api/v1/spending?category={id}). Only one of the two queries
 * is enabled at a time.
 */
export function SpendingDashboard() {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [categoryId, setCategoryId] = useState<string>(ALL_CATEGORIES_VALUE);

  const hasRange = from !== "" && to !== "";
  const hasCategory = categoryId !== ALL_CATEGORIES_VALUE;

  const categoriesQuery = useQuery({
    queryKey: CATEGORIES_QUERY_KEY,
    queryFn: listCategories,
  });

  const breakdownQuery = useQuery({
    queryKey: ["spending", "breakdown", { from, to }],
    queryFn: () => getSpendingBreakdown({ from, to }),
    enabled: hasRange && !hasCategory,
  });

  const singleCategoryQuery = useQuery({
    queryKey: ["spending", "category", { categoryId, from, to }],
    queryFn: () => getSpendingForCategory({ category: Number(categoryId), from, to }),
    enabled: hasRange && hasCategory,
  });

  const categories = categoriesQuery.data ?? [];
  const activeQuery = hasCategory ? singleCategoryQuery : breakdownQuery;

  return (
    <div className="mx-auto mt-8 max-w-4xl rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-semibold text-gray-900">Spending dashboard</h2>
      <p className="mt-1 text-sm text-gray-500">
        Choose a date range to see total spending per category, or narrow to a single category.
      </p>

      <div className="mt-4 flex flex-wrap items-end gap-4">
        <label className="flex flex-col text-sm text-gray-700">
          From
          <input
            type="date"
            value={from}
            onChange={(event) => setFrom(event.target.value)}
            className="mt-1 rounded-md border border-gray-300 px-2 py-1 text-sm"
          />
        </label>

        <label className="flex flex-col text-sm text-gray-700">
          To
          <input
            type="date"
            value={to}
            onChange={(event) => setTo(event.target.value)}
            className="mt-1 rounded-md border border-gray-300 px-2 py-1 text-sm"
          />
        </label>

        <label className="flex flex-col text-sm text-gray-700">
          Category
          <select
            aria-label="Category"
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value)}
            className="mt-1 rounded-md border border-gray-300 px-2 py-1 text-sm"
          >
            <option value={ALL_CATEGORIES_VALUE}>All categories</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="mt-4">
        {!hasRange && (
          <p className="text-gray-600">Pick a date range to see spending totals.</p>
        )}

        {hasRange && activeQuery.isLoading && (
          <p className="text-gray-600">Loading spending totals...</p>
        )}

        {hasRange && activeQuery.isError && (
          <p className="text-red-700">
            {errorMessage(activeQuery.error) ?? "Failed to load spending totals."}
          </p>
        )}

        {hasRange &&
          !hasCategory &&
          breakdownQuery.isSuccess &&
          breakdownQuery.data.breakdown.length === 0 && (
            <p className="text-gray-600">No spending found for this range.</p>
          )}

        {hasRange &&
          !hasCategory &&
          breakdownQuery.isSuccess &&
          breakdownQuery.data.breakdown.length > 0 && (
            <table className="mt-2 w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-gray-500">
                  <th className="py-2 pr-4 font-medium">Category</th>
                  <th className="py-2 pr-4 font-medium">Total spent</th>
                </tr>
              </thead>
              <tbody>
                {breakdownQuery.data.breakdown.map((entry) => (
                  <tr key={entry.categoryId} className="border-b border-gray-100">
                    <td className="py-2 pr-4">{entry.categoryName}</td>
                    <td className="py-2 pr-4">{entry.totalSpent}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

        {hasRange && hasCategory && singleCategoryQuery.isSuccess && (
          <div className="mt-2 text-sm text-gray-900">
            <span className="font-medium">{singleCategoryQuery.data.categoryName}</span>:{" "}
            {singleCategoryQuery.data.totalSpent}
          </div>
        )}
      </div>
    </div>
  );
}
