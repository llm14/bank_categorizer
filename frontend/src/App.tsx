import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { HealthStatus } from "./components/HealthStatus";
import { UploadStatement } from "./components/UploadStatement";
import { ManageCategories } from "./components/ManageCategories";
import { ReviewTransactions } from "./components/ReviewTransactions";
import { SpendingDashboard } from "./components/SpendingDashboard";
import { SpendingComparison } from "./components/SpendingComparison";
import { LandingPage } from "./components/LandingPage";
import type { Section } from "./components/LandingPage";

const queryClient = new QueryClient();

function App() {
  const [activeSection, setActiveSection] = useState<Section | null>(null);

  return (
    <QueryClientProvider client={queryClient}>
      <div className="min-h-screen bg-gray-50 pb-16">
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
