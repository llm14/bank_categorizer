import { useQuery } from "@tanstack/react-query";
import { getHealth } from "../api/health";

/**
 * Minimal connectivity screen (FE-2): calls GET /actuator/health through the typed API
 * client on load and reports whether the browser can actually reach the backend - proof
 * that the CORS setup (FE-1) works end-to-end, not just in tests.
 */
export function HealthStatus() {
  const { data, error, isLoading, isError } = useQuery({
    queryKey: ["health"],
    queryFn: getHealth,
  });

  return (
    <div className="mx-auto mt-16 max-w-md rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h1 className="text-xl font-semibold text-gray-900">Bank Categorizer</h1>
      <p className="mt-1 text-sm text-gray-500">Backend connectivity check</p>

      <div className="mt-4 flex items-center gap-2" role="status">
        {isLoading && (
          <>
            <span className="h-3 w-3 rounded-full bg-gray-400" aria-hidden="true" />
            <span className="text-gray-600">Checking connection...</span>
          </>
        )}

        {isError && (
          <>
            <span className="h-3 w-3 rounded-full bg-red-500" aria-hidden="true" />
            <span className="text-red-700">
              Disconnected
              {error instanceof Error ? `: ${error.message}` : null}
            </span>
          </>
        )}

        {!isLoading && !isError && (
          <>
            <span className="h-3 w-3 rounded-full bg-green-500" aria-hidden="true" />
            <span className="text-green-700">Connected (status: {data?.status})</span>
          </>
        )}
      </div>
    </div>
  );
}
