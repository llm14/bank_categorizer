import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { vi } from "vitest";
import App from "./App";
import { setToken } from "./api/authToken";

vi.mock("./api/auth", () => ({
  login: vi.fn(),
  logout: vi.fn().mockResolvedValue(undefined),
}));
vi.mock("./components/HealthStatus", () => ({
  HealthStatus: () => <div>health-status-stub</div>,
}));
vi.mock("./components/UploadStatement", () => ({
  UploadStatement: () => <div>upload-statement-stub</div>,
}));
vi.mock("./components/ManageCategories", () => ({
  ManageCategories: () => <div>manage-categories-stub</div>,
}));
vi.mock("./components/ReviewTransactions", () => ({
  ReviewTransactions: () => <div>review-transactions-stub</div>,
}));
vi.mock("./components/SpendingDashboard", () => ({
  SpendingDashboard: () => <div>spending-dashboard-stub</div>,
}));
vi.mock("./components/SpendingComparison", () => ({
  SpendingComparison: () => <div>spending-comparison-stub</div>,
}));

const SECTION_STUBS = [
  "manage-categories-stub",
  "review-transactions-stub",
  "spending-dashboard-stub",
  "spending-comparison-stub",
];

describe("App authentication gating (FE-11)", () => {
  afterEach(() => {
    setToken(null);
  });

  it("shows only the login screen when no token is stored", () => {
    render(<App />);

    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument();

    expect(screen.queryByText("health-status-stub")).not.toBeInTheDocument();
    expect(screen.queryByText("upload-statement-stub")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Manage categories" })).not.toBeInTheDocument();
  });

  it("skips the login screen when a token already exists (e.g. after a page refresh)", () => {
    setToken("existing-token");

    render(<App />);

    expect(screen.queryByRole("button", { name: /sign in/i })).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Bank Categorizer" })).toBeInTheDocument();
  });

  it("logging out clears the token and returns to the login screen", async () => {
    setToken("existing-token");
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole("button", { name: /log out/i }));

    expect(await screen.findByRole("button", { name: /sign in/i })).toBeInTheDocument();
  });
});

describe("App navigation (FE-8)", () => {
  beforeEach(() => {
    setToken("test-token");
  });

  afterEach(() => {
    setToken(null);
  });

  it("defaults to the landing page: app name, four buttons, no section content", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "Bank Categorizer" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Manage categories" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Review transactions" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Spending dashboard" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Spending comparison" })).toBeInTheDocument();

    for (const stub of SECTION_STUBS) {
      expect(screen.queryByText(stub)).not.toBeInTheDocument();
    }
  });

  it.each([
    ["Manage categories", "manage-categories-stub"],
    ["Review transactions", "review-transactions-stub"],
    ["Spending dashboard", "spending-dashboard-stub"],
    ["Spending comparison", "spending-comparison-stub"],
  ])("clicking \"%s\" shows only that section", async (buttonLabel, expectedStub) => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole("button", { name: buttonLabel }));

    expect(screen.getByText(expectedStub)).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Bank Categorizer" }),
    ).not.toBeInTheDocument();

    for (const stub of SECTION_STUBS) {
      if (stub !== expectedStub) {
        expect(screen.queryByText(stub)).not.toBeInTheDocument();
      }
    }
  });

  it("returns to the landing page via the back control", async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole("button", { name: "Manage categories" }));
    expect(screen.getByText("manage-categories-stub")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /back/i }));

    expect(screen.getByRole("heading", { name: "Bank Categorizer" })).toBeInTheDocument();
    for (const stub of SECTION_STUBS) {
      expect(screen.queryByText(stub)).not.toBeInTheDocument();
    }
  });
});
