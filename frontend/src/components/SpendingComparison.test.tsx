import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SpendingComparison } from "./SpendingComparison";
import { listCategories } from "../api/categories";
import { ApiError } from "../api/client";
import { compareSpending } from "../api/spending";
import type { CategoryResponse, SpendingComparisonResponse } from "../api/types";

vi.mock("../api/categories");
vi.mock("../api/spending");

const mockedListCategories = vi.mocked(listCategories);
const mockedCompareSpending = vi.mocked(compareSpending);

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <SpendingComparison />
    </QueryClientProvider>,
  );
}

function category(overrides: Partial<CategoryResponse> = {}): CategoryResponse {
  return {
    id: 1,
    name: "Groceries",
    description: null,
    keywords: [],
    ...overrides,
  };
}

function comparison(
  overrides: Partial<SpendingComparisonResponse> = {},
): SpendingComparisonResponse {
  return {
    categoryId: 1,
    categoryName: "Groceries",
    period: "month",
    lookback: 3,
    current: { label: "2026-07", from: "2026-07-01", to: "2026-07-31", totalSpent: 120 },
    previousPeriods: [
      { label: "2026-06", from: "2026-06-01", to: "2026-06-30", totalSpent: 100 },
      { label: "2026-05", from: "2026-05-01", to: "2026-05-31", totalSpent: 80 },
      { label: "2026-04", from: "2026-04-01", to: "2026-04-30", totalSpent: 90 },
    ],
    previousAverage: 90,
    ...overrides,
  };
}

describe("SpendingComparison", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows a neutral prompt when no category has been picked yet", async () => {
    mockedListCategories.mockResolvedValue([category()]);

    renderWithClient();

    expect(await screen.findByText(/pick a category/i)).toBeInTheDocument();
    expect(mockedCompareSpending).not.toHaveBeenCalled();
  });

  it("renders the current period, previous periods, and average once a category is picked", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedCompareSpending.mockResolvedValue(comparison());

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByRole("option", { name: "Groceries" });
    await user.selectOptions(screen.getByLabelText(/^category$/i), "1");

    expect(await screen.findByText(/2026-07/)).toBeInTheDocument();
    expect(screen.getByText(/120/)).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "2026-06" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "100" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "2026-05" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "80" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "2026-04" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "90" })).toBeInTheDocument();
    expect(screen.getByText(/previous average/i)).toBeInTheDocument();

    expect(mockedCompareSpending).toHaveBeenCalledWith({
      category: 1,
      period: "month",
      lookback: 3,
    });
  });

  it("re-queries when the lookback value changes", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedCompareSpending.mockResolvedValue(comparison());

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByRole("option", { name: "Groceries" });
    await user.selectOptions(screen.getByLabelText(/^category$/i), "1");
    await screen.findByText(/2026-07/);
    expect(mockedCompareSpending).toHaveBeenCalledTimes(1);

    const lookbackInput = screen.getByLabelText(/lookback/i);
    await user.clear(lookbackInput);
    await user.type(lookbackInput, "6");

    expect(await screen.findByText(/2026-07/)).toBeInTheDocument();
    expect(mockedCompareSpending).toHaveBeenLastCalledWith({
      category: 1,
      period: "month",
      lookback: 6,
    });
  });

  it("shows the backend's actual message for an out-of-range lookback (400)", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedCompareSpending.mockRejectedValue(
      new ApiError(400, {
        timestamp: "2026-07-10T00:00:00Z",
        status: 400,
        error: "Bad Request",
        message: "'lookback' must not exceed 24 months",
      }),
    );

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByRole("option", { name: "Groceries" });
    await user.selectOptions(screen.getByLabelText(/^category$/i), "1");

    expect(
      await screen.findByText(/'lookback' must not exceed 24 months/i),
    ).toBeInTheDocument();
  });
});
