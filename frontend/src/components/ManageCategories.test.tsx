import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ManageCategories } from "./ManageCategories";
import { createCategory, deleteCategory, listCategories } from "../api/categories";
import { ApiError } from "../api/client";
import type { CategoryResponse } from "../api/types";

vi.mock("../api/categories");

const mockedListCategories = vi.mocked(listCategories);
const mockedCreateCategory = vi.mocked(createCategory);
const mockedDeleteCategory = vi.mocked(deleteCategory);

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ManageCategories />
    </QueryClientProvider>,
  );
}

function category(overrides: Partial<CategoryResponse> = {}): CategoryResponse {
  return {
    id: 1,
    name: "Groceries",
    description: "Food shopping",
    keywords: ["walmart", "costco"],
    ...overrides,
  };
}

describe("ManageCategories", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("lists categories including their keywords", async () => {
    mockedListCategories.mockResolvedValue([
      category(),
      category({ id: 2, name: "Dining", description: null, keywords: [] }),
    ]);

    renderWithClient();

    expect(await screen.findByText("Groceries")).toBeInTheDocument();
    expect(screen.getByText("Food shopping")).toBeInTheDocument();
    expect(screen.getByText("walmart, costco")).toBeInTheDocument();
    expect(screen.getByText("Dining")).toBeInTheDocument();
    expect(screen.getByText("No keywords")).toBeInTheDocument();
  });

  it("shows a loading state while categories are fetched", () => {
    mockedListCategories.mockReturnValue(new Promise(() => {}));

    renderWithClient();

    expect(screen.getByText(/loading categories/i)).toBeInTheDocument();
  });

  it("shows an empty state when there are no categories", async () => {
    mockedListCategories.mockResolvedValue([]);

    renderWithClient();

    expect(await screen.findByText(/no categories yet/i)).toBeInTheDocument();
  });

  it("shows the backend's error message when the categories list fails to load", async () => {
    mockedListCategories.mockRejectedValue(
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

  it("creates a category, splitting keywords, and refreshes the list on success", async () => {
    mockedListCategories.mockResolvedValueOnce([]).mockResolvedValueOnce([
      category({ id: 3, name: "Travel", description: "Trips", keywords: ["airbnb", "delta"] }),
    ]);
    mockedCreateCategory.mockResolvedValue(
      category({ id: 3, name: "Travel", description: "Trips", keywords: ["airbnb", "delta"] }),
    );

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByText(/no categories yet/i);

    await user.type(screen.getByLabelText(/^name$/i), "Travel");
    await user.type(screen.getByLabelText(/description/i), "Trips");
    await user.type(screen.getByLabelText(/keywords/i), "airbnb, delta");
    await user.click(screen.getByRole("button", { name: /create category/i }));

    expect(mockedCreateCategory).toHaveBeenCalledWith(
      {
        name: "Travel",
        description: "Trips",
        keywords: ["airbnb", "delta"],
      },
      expect.anything(),
    );

    expect(await screen.findByText("Travel")).toBeInTheDocument();
    expect(mockedListCategories).toHaveBeenCalledTimes(2);
  });

  it("shows the real backend message for a duplicate category name (409)", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedCreateCategory.mockRejectedValue(
      new ApiError(409, {
        timestamp: "2026-07-10T00:00:00Z",
        status: 409,
        error: "Conflict",
        message: "A category named 'Groceries' already exists",
      }),
    );

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByText("Groceries");

    await user.type(screen.getByLabelText(/^name$/i), "Groceries");
    await user.click(screen.getByRole("button", { name: /create category/i }));

    expect(await screen.findByText(/a category named 'groceries' already exists/i)).toBeInTheDocument();
  });

  it("shows the real backend message for a validation failure (400)", async () => {
    mockedListCategories.mockResolvedValue([]);
    mockedCreateCategory.mockRejectedValue(
      new ApiError(400, {
        timestamp: "2026-07-10T00:00:00Z",
        status: 400,
        error: "Bad Request",
        message: "name must be at most 255 characters",
      }),
    );

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByText(/no categories yet/i);

    await user.type(screen.getByLabelText(/^name$/i), "x".repeat(300));
    await user.click(screen.getByRole("button", { name: /create category/i }));

    expect(await screen.findByText(/name must be at most 255 characters/i)).toBeInTheDocument();
  });

  it("does not delete without confirmation, and deletes + refreshes the list once confirmed", async () => {
    mockedListCategories.mockResolvedValue([category()]);
    mockedDeleteCategory.mockResolvedValue(undefined);

    renderWithClient();
    const user = userEvent.setup();

    await screen.findByText("Groceries");

    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValueOnce(false);
    await user.click(screen.getByRole("button", { name: /delete/i }));

    expect(confirmSpy).toHaveBeenCalledWith(
      expect.stringMatching(/uncategorized, not deleted/i),
    );
    expect(mockedDeleteCategory).not.toHaveBeenCalled();

    confirmSpy.mockReturnValueOnce(true);
    mockedListCategories.mockResolvedValueOnce([]);
    await user.click(screen.getByRole("button", { name: /delete/i }));

    expect(mockedDeleteCategory).toHaveBeenCalledWith(1, expect.anything());
    expect(await screen.findByText(/no categories yet/i)).toBeInTheDocument();
  });
});
