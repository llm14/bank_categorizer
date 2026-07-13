import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LoginScreen } from "./LoginScreen";
import { login } from "../api/auth";
import { ApiError } from "../api/client";
import { getToken, setToken } from "../api/authToken";

vi.mock("../api/auth");

const mockedLogin = vi.mocked(login);

function renderWithClient(onLoginSuccess = vi.fn()) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    onLoginSuccess,
    ...render(
      <QueryClientProvider client={queryClient}>
        <LoginScreen onLoginSuccess={onLoginSuccess} />
      </QueryClientProvider>,
    ),
  };
}

describe("LoginScreen", () => {
  afterEach(() => {
    setToken(null);
  });

  it("logs in successfully, stores the token, and notifies the parent", async () => {
    mockedLogin.mockResolvedValue({ token: "abc123" });
    const user = userEvent.setup();
    const { onLoginSuccess } = renderWithClient();

    await user.type(screen.getByLabelText(/username/i), "admin");
    await user.type(screen.getByLabelText(/password/i), "admin");
    await user.click(screen.getByRole("button", { name: /sign in/i }));

    expect(mockedLogin).toHaveBeenCalledWith(
      { username: "admin", password: "admin" },
      expect.anything(),
    );
    expect(await screen.findByRole("button")).toBeInTheDocument();
    expect(onLoginSuccess).toHaveBeenCalled();
    expect(getToken()).toBe("abc123");
  });

  it("shows the backend's real error message on failed login", async () => {
    mockedLogin.mockRejectedValue(
      new ApiError(401, {
        timestamp: "2026-07-13T00:00:00Z",
        status: 401,
        error: "Unauthorized",
        message: "Invalid username or password",
      }),
    );
    const user = userEvent.setup();
    const { onLoginSuccess } = renderWithClient();

    await user.type(screen.getByLabelText(/username/i), "admin");
    await user.type(screen.getByLabelText(/password/i), "wrong");
    await user.click(screen.getByRole("button", { name: /sign in/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password");
    expect(onLoginSuccess).not.toHaveBeenCalled();
    expect(getToken()).toBeNull();
  });
});
