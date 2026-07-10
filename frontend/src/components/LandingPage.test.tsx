import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LandingPage } from "./LandingPage";

describe("LandingPage", () => {
  it("renders the app name and the four section buttons", () => {
    render(<LandingPage onNavigate={vi.fn()} />);

    expect(screen.getByRole("heading", { name: "Bank Categorizer" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Manage categories" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Review transactions" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Spending dashboard" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Spending comparison" })).toBeInTheDocument();
  });

  it.each([
    ["Manage categories", "categories"],
    ["Review transactions", "transactions"],
    ["Spending dashboard", "dashboard"],
    ["Spending comparison", "comparison"],
  ] as const)("calls onNavigate with %s's section when clicked", async (label, section) => {
    const onNavigate = vi.fn();
    const user = userEvent.setup();
    render(<LandingPage onNavigate={onNavigate} />);

    await user.click(screen.getByRole("button", { name: label }));

    expect(onNavigate).toHaveBeenCalledWith(section);
  });
});
