import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { HealthStatus } from "./HealthStatus";
import { getHealth } from "../api/health";
import { ApiError } from "../api/client";

vi.mock("../api/health");

const mockedGetHealth = vi.mocked(getHealth);

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <HealthStatus />
    </QueryClientProvider>,
  );
}

describe("HealthStatus", () => {
  it("shows a connected status when the backend health check succeeds", async () => {
    mockedGetHealth.mockResolvedValue({ status: "UP" });

    renderWithClient();

    expect(await screen.findByText(/connected \(status: up\)/i)).toBeInTheDocument();
  });

  it("shows a disconnected status with the error message when the backend health check fails", async () => {
    mockedGetHealth.mockRejectedValue(new ApiError(503, {
      timestamp: "2026-07-10T00:00:00Z",
      status: 503,
      error: "Service Unavailable",
      message: "Database connection is down",
    }));

    renderWithClient();

    expect(await screen.findByText(/disconnected/i)).toBeInTheDocument();
    expect(await screen.findByText(/database connection is down/i)).toBeInTheDocument();
  });
});
