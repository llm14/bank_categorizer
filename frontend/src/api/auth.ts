import { request } from "./client";
import type { LoginRequest, LoginResponse } from "./types";

/**
 * POST /api/v1/auth/login - the only endpoint reachable without a bearer token. Throws
 * ApiError(401) with the backend's generic "Invalid username or password" message on bad
 * credentials.
 */
export function login(body: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/**
 * POST /api/v1/auth/logout - invalidates the current bearer token server-side (sent
 * automatically via client.ts's Authorization header). Returns 204/void.
 */
export function logout(): Promise<void> {
  return request<void>("/api/v1/auth/logout", { method: "POST" });
}
