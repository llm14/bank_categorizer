import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import type { FormEvent } from "react";
import { login } from "../api/auth";
import { ApiError } from "../api/client";
import { setToken } from "../api/authToken";

interface LoginScreenProps {
  onLoginSuccess: () => void;
}

function errorMessage(error: unknown): string | null {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return null;
}

/**
 * Login screen (FE-11): the only thing rendered until authenticated (see App.tsx). Posts
 * username/password to POST /api/v1/auth/login; on success stores the returned bearer
 * token (authToken.ts, backed by sessionStorage) and notifies the parent so it can switch
 * to the authenticated app shell. On failure (401) renders the backend's real error
 * message rather than a generic string.
 */
export function LoginScreen({ onLoginSuccess }: LoginScreenProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setToken(data.token);
      onLoginSuccess();
    },
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    loginMutation.mutate({ username, password });
  }

  return (
    <div className="mx-auto mt-16 max-w-md rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h1 className="text-xl font-semibold text-gray-900">Bank Categorizer</h1>
      <p className="mt-1 text-sm text-gray-500">Sign in to continue.</p>

      <form className="mt-4 space-y-3" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="login-username" className="block text-sm font-medium text-gray-700">
            Username
          </label>
          <input
            id="login-username"
            type="text"
            required
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
          />
        </div>

        <div>
          <label htmlFor="login-password" className="block text-sm font-medium text-gray-700">
            Password
          </label>
          <input
            id="login-password"
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
          />
        </div>

        <button
          type="submit"
          disabled={!username.trim() || !password.trim() || loginMutation.isPending}
          className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300"
        >
          {loginMutation.isPending ? "Signing in..." : "Sign in"}
        </button>

        {loginMutation.isError && (
          <p role="alert" className="text-sm text-red-700">
            {errorMessage(loginMutation.error) ?? "Login failed."}
          </p>
        )}
      </form>
    </div>
  );
}
