import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { HealthStatus } from "./components/HealthStatus";

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="min-h-screen bg-gray-50">
        <HealthStatus />
      </div>
    </QueryClientProvider>
  );
}

export default App;
