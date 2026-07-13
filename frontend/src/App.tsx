import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { HealthStatus } from "./components/HealthStatus";
import { UploadStatement } from "./components/UploadStatement";
import { ManageCategories } from "./components/ManageCategories";
import { ReviewTransactions } from "./components/ReviewTransactions";
import { SpendingDashboard } from "./components/SpendingDashboard";
import { SpendingComparison } from "./components/SpendingComparison";
import { LandingPage } from "./components/LandingPage";
import type { Section } from "./components/LandingPage";
import { LoginScreen } from "./components/LoginScreen";
import { logout as logoutRequest } from "./api/auth";
import { getToken, setToken, setUnauthorizedHandler } from "./api/authToken";

const queryClient = new QueryClient();

function App() {
  const [activeSection, setActiveSection] = useState<Section | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => getToken() !== null);

  // Any 401 from any endpoint (e.g. an expired token) drops the user back to the login
  // form, regardless of which screen triggered it - see api/authToken.ts and client.ts.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setToken(null);
      setIsAuthenticated(false);
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  function handleLogout() {
    // Best-effort: invalidate the token server-side, but log out locally regardless of
    // whether the request succeeds (e.g. the token already expired).
    void logoutRequest().catch(() => {});
    setToken(null);
    setIsAuthenticated(false);
    setActiveSection(null);
  }

  if (!isAuthenticated) {
    return (
      <QueryClientProvider client={queryClient}>
        <div className="min-h-screen bg-gray-50 pb-16">
          <LoginScreen onLoginSuccess={() => setIsAuthenticated(true)} />
        </div>
      </QueryClientProvider>
    );
  }

  return (
    <QueryClientProvider client={queryClient}>
      <div className="min-h-screen bg-gray-50 pb-16">
        <div className="mx-auto mt-4 flex max-w-4xl justify-end px-4">
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
          >
            Log out
          </button>
        </div>

        <HealthStatus />
        <UploadStatement />

        {activeSection === null && <LandingPage onNavigate={setActiveSection} />}

        {activeSection !== null && (
          <div className="mx-auto mt-8 max-w-4xl">
            <button
              type="button"
              onClick={() => setActiveSection(null)}
              className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
            >
              &larr; Back to home
            </button>
          </div>
        )}

        {activeSection === "categories" && <ManageCategories />}
        {activeSection === "transactions" && <ReviewTransactions />}
        {activeSection === "dashboard" && <SpendingDashboard />}
        {activeSection === "comparison" && <SpendingComparison />}
      </div>
    </QueryClientProvider>
  );
}

export default App;
