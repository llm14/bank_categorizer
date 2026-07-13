import type { ErrorResponse } from "./types";
import { getToken, notifyUnauthorized } from "./authToken";

/**
 * Base URL of the backend API. Always read from VITE_API_BASE_URL - never hardcode a
 * host/port here. See frontend/.env.example.
 */
export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

/**
 * Thrown for any non-2xx response. Wraps the backend's actual error body
 * (com.bankcategorizer.dto.ErrorResponse) when the response has one, so callers can surface
 * the real `message` field instead of a generic failure string.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly body?: ErrorResponse;

  constructor(status: number, body?: ErrorResponse) {
    super(body?.message ?? `Request failed with status ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

async function parseErrorBody(response: Response): Promise<ErrorResponse | undefined> {
  try {
    return (await response.json()) as ErrorResponse;
  } catch {
    return undefined;
  }
}

/** Attaches `Authorization: Bearer <token>` when a token is currently stored (see authToken.ts). */
function authHeaders(): Record<string, string> {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * Builds and throws the ApiError for a non-ok response, first notifying the registered
 * "unauthorized" handler on a 401 so the app can drop back to the login screen regardless
 * of which screen/endpoint triggered it. Still throws afterward so the calling screen's
 * existing error handling (e.g. rendering `error.message`) is unaffected.
 */
async function throwForResponse(response: Response): Promise<never> {
  const body = await parseErrorBody(response);
  if (response.status === 401) {
    notifyUnauthorized();
  }
  throw new ApiError(response.status, body);
}

/** Low-level JSON request helper shared by every typed endpoint function below. */
export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...authHeaders(),
      ...init?.headers,
    },
  });

  if (!response.ok) {
    return throwForResponse(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

/** Same as {@link request}, but for multipart/form-data bodies (e.g. file uploads). */
export async function requestFormData<T>(path: string, formData: FormData, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    method: init?.method ?? "POST",
    body: formData,
    headers: {
      Accept: "application/json",
      ...authHeaders(),
      ...init?.headers,
    },
  });

  if (!response.ok) {
    return throwForResponse(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

function buildQuery(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined) {
      search.set(key, String(value));
    }
  }
  const query = search.toString();
  return query ? `?${query}` : "";
}

export { buildQuery };
