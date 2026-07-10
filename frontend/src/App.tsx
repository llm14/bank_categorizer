import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { HealthStatus } from "./components/HealthStatus";
import { UploadStatement } from "./components/UploadStatement";
import { ManageCategories } from "./components/ManageCategories";
import { ReviewTransactions } from "./components/ReviewTransactions";

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="min-h-screen bg-gray-50">
        <HealthStatus />
        <UploadStatement />
        <ManageCategories />
        <ReviewTransactions />
      </div>
    </QueryClientProvider>
  );
}

export default App;
