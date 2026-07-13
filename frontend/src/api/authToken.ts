/**
 * Shared bearer-token storage for the API client (FE-11 / US-13).
 *
 * The token is kept in `sessionStorage` (not just component state) so a page refresh
 * doesn't force the user to log in again, while still clearing when the tab closes.
 * `client.ts`'s `request`/`requestFormData` read it via {@link getToken} to attach an
 * `Authorization: Bearer <token>` header; the login flow writes it via {@link setToken}.
 */

const TOKEN_STORAGE_KEY = "bank-categorizer.authToken";

let unauthorizedHandler: (() => void) | null = null;

export function getToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null): void {
  try {
    if (token === null) {
      sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    } else {
      sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
    }
  } catch {
    // sessionStorage unavailable (e.g. disabled) - nothing else to do, token just
    // won't survive a refresh.
  }
}

/**
 * Registers the callback `request`/`requestFormData` invoke whenever a response comes
 * back 401 - i.e. "any endpoint anywhere just rejected the current token". `App.tsx`
 * registers this once to clear the token and drop back to the login screen, regardless
 * of which screen triggered the 401.
 */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

/** Invoked by `client.ts` right before it throws a 401 `ApiError`. */
export function notifyUnauthorized(): void {
  unauthorizedHandler?.();
}
