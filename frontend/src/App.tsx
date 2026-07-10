import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { HealthStatus } from "./components/HealthStatus";
import { UploadStatement } from "./components/UploadStatement";
import { ManageCategories } from "./components/ManageCategories";
import { ReviewTransactions } from "./components/ReviewTransactions";
import { SpendingDashboard } from "./components/SpendingDashboard";

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="min-h-screen bg-gray-50">
        <HealthStatus />
        <UploadStatement />
        <ManageCategories />
        <ReviewTransactions />
        <SpendingDashboard />
      </div>
    </QueryClientProvider>
  );
}

export default App;
