import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SpendingDashboard } from "./SpendingDashboard";
import { listCategories } from "../api/categories";
import { ApiError } from "../api/client";
import { getSpendingBreakdown, getSpendingForCategory } from "../api/spending";
import type { CategoryResponse, SpendingBreakdownResponse, SpendingResponse } from "../api/types";

vi.mock("../api/categories");
vi.mock("../api/spending");

const mockedListCategories = vi.mocked(listCategories);
const mockedGetSpendingBreakdown = vi.mocked(getSpendingBreakdown);
const mockedGetSpendingForCategory = vi.mocked(getSpendingForCategory);

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <SpendingDashboard />
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

function spending(overrides: Partial<SpendingResponse> = {}): SpendingResponse {
  return {
    categoryId: 1,
    categoryName: "Groceries",
    from: "2026-07-01",
    to: "2026-07-31",
    totalSpent: 123.45,
    ...overrides,
  };
}

function breakdown(entries: SpendingResponse[], totalSpent = 0): SpendingBreakdownResponse {
  return { breakdown: entries, totalSpent };
}

function setRange(from: string, to: string) {
  const fromInput = screen.getByLabelText(/^from$/i);
  const toInput = screen.getByLabelText(/^to$/i);
  fireEvent.change(fromInput, { target: { value: from } });
  fireEvent.change(toInput, { target: { value: to } });
}

describe("SpendingDashboard", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows a neutral prompt when no date range has been picked yet", async () => {
    mockedListCategories.mockResolvedValue([category()]);

    renderWithClient();

    expect(await screen.findByText(/pick a date range/i)).toBeInTheDocument();
    expect(mockedGetSpendingBreakdown).not.toHaveBeenCalled();
    expect(mockedGetSpendingForCategory).not.toHaveBeenCalled();
  });

  it("renders the breakdown across categories once both dates are filled in", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedGetSpendingBreakdown.mockResolvedValue(
      breakdown(
        [
          spending({ categoryId: 1, categoryName: "Groceries", totalSpent: 100 }),
          spending({ categoryId: 2, categoryName: "Dining", totalSpent: 50 }),
        ],
        150,
      ),
    );

    renderWithClient();
    setRange("2026-07-01", "2026-07-31");

    expect(await screen.findByRole("cell", { name: "Groceries" })).toBeInTheDocument();
    expect(screen.getByText("100")).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "Dining" })).toBeInTheDocument();
    expect(screen.getByText("50")).toBeInTheDocument();
    expect(mockedGetSpendingForCategory).not.toHaveBeenCalled();
  });

  it("renders a single category's total when a category is selected", async () => {
    mockedListCategories.mockResolvedValue([
      category({ id: 1, name: "Groceries" }),
      category({ id: 2, name: "Dining" }),
    ]);
    mockedGetSpendingBreakdown.mockResolvedValue(breakdown([spending()], 123.45));
    mockedGetSpendingForCategory.mockResolvedValue(
      spending({ categoryId: 2, categoryName: "Dining", totalSpent: 75 }),
    );

    renderWithClient();
    const user = userEvent.setup();
    setRange("2026-07-01", "2026-07-31");

    await screen.findByRole("cell", { name: "Groceries" });

    await user.selectOptions(screen.getByLabelText(/^category$/i), "2");

    expect(await screen.findByText(/75/)).toBeInTheDocument();
    expect(mockedGetSpendingForCategory).toHaveBeenCalledWith({
      category: 2,
      from: "2026-07-01",
      to: "2026-07-31",
    });
  });

  it("switches from breakdown to single-category query without running both at once", async () => {
    mockedListCategories.mockResolvedValue([
      category({ id: 1, name: "Groceries" }),
      category({ id: 2, name: "Dining" }),
    ]);
    mockedGetSpendingBreakdown.mockResolvedValue(breakdown([spending()], 123.45));
    mockedGetSpendingForCategory.mockResolvedValue(
      spending({ categoryId: 2, categoryName: "Dining", totalSpent: 75 }),
    );

    renderWithClient();
    const user = userEvent.setup();
    setRange("2026-07-01", "2026-07-31");

    await screen.findByRole("cell", { name: "Groceries" });
    expect(mockedGetSpendingBreakdown).toHaveBeenCalledTimes(1);
    expect(mockedGetSpendingForCategory).not.toHaveBeenCalled();

    await user.selectOptions(screen.getByLabelText(/^category$/i), "2");

    await screen.findByText(/75/);
    expect(mockedGetSpendingForCategory).toHaveBeenCalledTimes(1);
    expect(mockedGetSpendingBreakdown).toHaveBeenCalledTimes(1);

    await user.selectOptions(screen.getByLabelText(/^category$/i), "");

    await screen.findByRole("cell", { name: "Groceries" });
    expect(mockedGetSpendingBreakdown).toHaveBeenCalledTimes(2);
    expect(mockedGetSpendingForCategory).toHaveBeenCalledTimes(1);
  });

  it("shows the backend's error message for an invalid date range (400)", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedGetSpendingBreakdown.mockRejectedValue(
      new ApiError(400, {
        timestamp: "2026-07-10T00:00:00Z",
        status: 400,
        error: "Bad Request",
        message: "'from' date must not be after 'to' date",
      }),
    );

    renderWithClient();
    setRange("2026-07-31", "2026-07-01");

    expect(await screen.findByText(/'from' date must not be after 'to' date/i)).toBeInTheDocument();
  });

  it("shows the backend's error message for an unknown category (404)", async () => {
    mockedListCategories.mockResolvedValue([category({ id: 1, name: "Groceries" })]);
    mockedGetSpendingBreakdown.mockResolvedValue(breakdown([spending()], 123.45));
    mockedGetSpendingForCategory.mockRejectedValue(
      new ApiError(404, {
        timestamp: "2026-07-10T00:00:00Z",
        status: 404,
        error: "Not Found",
        message: "Category 1 not found",
      }),
    );

    renderWithClient();
    const user = userEvent.setup();
    setRange("2026-07-01", "2026-07-31");

    await screen.findByRole("cell", { name: "Groceries" });
    await user.selectOptions(screen.getByLabelText(/^category$/i), "1");

    expect(await screen.findByText(/category 1 not found/i)).toBeInTheDocument();
  });
});
