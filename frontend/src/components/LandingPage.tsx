export type Section = "categories" | "transactions" | "dashboard" | "comparison";

interface LandingPageProps {
  onNavigate: (section: Section) => void;
}

interface SectionButton {
  section: Section;
  label: string;
}

const SECTION_BUTTONS: SectionButton[] = [
  { section: "categories", label: "Manage categories" },
  { section: "transactions", label: "Review transactions" },
  { section: "dashboard", label: "Spending dashboard" },
  { section: "comparison", label: "Spending comparison" },
];

/**
 * Default landing view (FE-8): just the app name and one button per section screen
 * (ManageCategories/ReviewTransactions/SpendingDashboard/SpendingComparison from FE-4-FE-7).
 * Upload (FE-3) and the backend connectivity check (FE-2) are intentionally out of this
 * navigation - they're rendered elsewhere in the app shell (see App.tsx).
 */
export function LandingPage({ onNavigate }: LandingPageProps) {
  return (
    <div className="mx-auto mt-16 max-w-md text-center">
      <h1 className="text-2xl font-semibold text-gray-900">Bank Categorizer</h1>

      <div className="mt-8 flex flex-col gap-3">
        {SECTION_BUTTONS.map(({ section, label }) => (
          <button
            key={section}
            type="button"
            onClick={() => onNavigate(section)}
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
          >
            {label}
          </button>
        ))}
      </div>
    </div>
  );
}
