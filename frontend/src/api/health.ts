import { request } from "./client";
import type { HealthResponse } from "./types";

/**
 * GET /actuator/health - proves the browser can reach the backend across origins (see
 * CorsConfig, which opens this endpoint up alongside /api/v1/**).
 */
export function getHealth(): Promise<HealthResponse> {
  return request<HealthResponse>("/actuator/health");
}
