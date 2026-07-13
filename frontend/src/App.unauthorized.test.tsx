import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import { setToken } from "./api/authToken";

/**
 * Exercises the real end-to-end "any 401 anywhere drops back to login" mechanism (FE-11):
 * unlike App.test.tsx (which stubs out every screen component and its API calls), this
 * leaves App's real tree in place and only mocks `fetch` itself, so the actual
 * client.ts request() -> ApiError(401) -> notifyUnauthorized() -> App state flip chain runs
 * for real. HealthStatus's always-on GET /actuator/health call is what triggers it here,
 * standing in for "some other endpoint" (e.g. an expired token hitting any protected route).
 */
describe("App - unauthorized handling (FE-11)", () => {
  beforeEach(() => {
    setToken("stale-token");
  });

  afterEach(() => {
    setToken(null);
    vi.restoreAllMocks();
  });

  it("drops back to the login screen when any endpoint responds 401", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            timestamp: "2026-07-13T00:00:00Z",
            status: 401,
            error: "Unauthorized",
            message: "Invalid or expired token",
          }),
          { status: 401, headers: { "Content-Type": "application/json" } },
        ),
      ),
    );

    render(<App />);

    expect(await screen.findByRole("button", { name: /sign in/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /log out/i })).not.toBeInTheDocument();
  });
});
