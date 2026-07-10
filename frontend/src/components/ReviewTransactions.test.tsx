import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ReviewTransactions } from "./ReviewTransactions";
import { listCategories } from "../api/categories";
import { ApiError } from "../api/client";
import { listTransactions, updateTransactionCategory } from "../api/transactions";
import type { CategoryResponse, PageResponse, TransactionResponse } from "../api/types";

vi.mock("../api/categories");
vi.mock("../api/transactions");

const mockedListCategories = vi.mocked(listCategories);
const mockedListTransactions = vi.mocked(listTransactions);
const mockedUpdateTransactionCategory = vi.mocked(updateTransactionCategory);

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ReviewTransactions />
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

function transaction(overrides: Partial<TransactionResponse> = {}): TransactionResponse {
  return {
    id: 1,
    date: "2026-07-01",
    description: "Walmart",
    amount: -42.5,
    categoryId: 1,
    categoryName: "Groceries",
    ...overrides,
  };
}

function page(
  content: TransactionResponse[],
  overrides: Partial<PageResponse<TransactionResponse>> = {},
): PageResponse<TransactionResponse> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    ...overrides,
  };
}

describe("ReviewTransactions", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows a loading state while transactions are fetched", () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedListTransactions.mockReturnValue(new Promise(() => {}));

    renderWithClient();

    expect(screen.getByText(/loading transactions/i)).toBeInTheDocument();
  });

  it("renders categorized and uncategorized rows with signed amounts", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedListTransactions.mockResolvedValue(
      page([
        transaction({ id: 1, description: "Walmart", amount: -42.5, categoryId: 1, categoryName: "Groceries" }),
        transaction({ id: 2, description: "Salary", amount: 2000, categoryId: null, categoryName: null }),
      ]),
    );

    renderWithClient();

    expect(await screen.findByText("Walmart")).toBeInTheDocument();
    expect(screen.getByText("-42.5")).toBeInTheDocument();
    expect(screen.getByText("Salary")).toBeInTheDocument();
    expect(screen.getByText("2000")).toBeInTheDocument();

    const salaryPicker = screen.getByLabelText(/category for salary/i);
    expect(salaryPicker).toHaveValue("");

    const walmartPicker = screen.getByLabelText(/category for walmart/i);
    expect(walmartPicker).toHaveValue("1");
  });

  it("shows an empty state when there are no transactions", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedListTransactions.mockResolvedValue(page([]));

    renderWithClient();

    expect(await screen.findByText(/no transactions found/i)).toBeInTheDocument();
  });

  it("shows the backend's error message when the transactions list fails to load", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedListTransactions.mockRejectedValue(
      new ApiError(500, {
        timestamp: "2026-07-10T00:00:00Z",
        status: 500,
        error: "Internal Server Error",
        message: "Something went wrong upstream",
      }),
    );

    renderWithClient();

    expect(await screen.findByText(/something went wrong upstream/i)).toBeInTheDocument();
  });

  it("toggling the uncategorized-only filter refetches with the uncategorized filter and resets to page 0", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedListTransactions.mockResolvedValue(
      page([transaction({ id: 1, categoryId: null, categoryName: null })]),
    );

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByText("Walmart");

    expect(mockedListTransactions).toHaveBeenCalledWith(
      expect.objectContaining({ category: undefined, page: 0 }),
    );

    await user.click(screen.getByRole("checkbox", { name: /show uncategorized only/i }));

    expect(await screen.findByText("Walmart")).toBeInTheDocument();
    expect(mockedListTransactions).toHaveBeenLastCalledWith(
      expect.objectContaining({ category: "uncategorized", page: 0 }),
    );
  });

  it("changing a row's category calls the PATCH endpoint and refetches the list", async () => {
    mockedListCategories.mockResolvedValue([category({ id: 1, name: "Groceries" }), category({ id: 2, name: "Dining" })]);
    mockedListTransactions
      .mockResolvedValueOnce(
        page([transaction({ id: 1, description: "Walmart", categoryId: null, categoryName: null })]),
      )
      .mockResolvedValueOnce(
        page([transaction({ id: 1, description: "Walmart", categoryId: 2, categoryName: "Dining" })]),
      );
    mockedUpdateTransactionCategory.mockResolvedValue(
      transaction({ id: 1, description: "Walmart", categoryId: 2, categoryName: "Dining" }),
    );

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByText("Walmart");

    await user.selectOptions(screen.getByLabelText(/category for walmart/i), "2");

    expect(mockedUpdateTransactionCategory).toHaveBeenCalledWith(1, { categoryId: 2 });
    await screen.findByText("Dining");
    expect(mockedListTransactions).toHaveBeenCalledTimes(2);
  });

  it("shows the real backend error message when the PATCH fails", async () => {
    mockedListCategories.mockResolvedValue([category({ id: 1, name: "Groceries" }), category({ id: 2, name: "Dining" })]);
    mockedListTransactions.mockResolvedValue(
      page([transaction({ id: 1, description: "Walmart", categoryId: null, categoryName: null })]),
    );
    mockedUpdateTransactionCategory.mockRejectedValue(
      new ApiError(404, {
        timestamp: "2026-07-10T00:00:00Z",
        status: 404,
        error: "Not Found",
        message: "Transaction 1 not found",
      }),
    );

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByText("Walmart");

    await user.selectOptions(screen.getByLabelText(/category for walmart/i), "2");

    expect(await screen.findByText(/transaction 1 not found/i)).toBeInTheDocument();
  });
});
