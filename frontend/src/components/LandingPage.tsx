export type Section = "categories" | "transactions" | "dashboard" | "comparison";

interface LandingPageProps {
  onNavigate: (section: Section) => void;
}

interface SectionRow {
  section: Section;
  /** Short statement-style type code, echoing the transaction-type column (ACH/POS/ATM) on a real bank statement. */
  code: string;
  label: string;
  description: string;
}

const SECTION_ROWS: SectionRow[] = [
  {
    section: "categories",
    code: "ACCT",
    label: "Manage categories",
    description: "Add, rename, or remove the categories transactions are matched against.",
  },
  {
    section: "transactions",
    code: "TXN",
    label: "Review transactions",
    description: "Confirm auto-categorized transactions or fix the ones that slipped through.",
  },
  {
    section: "dashboard",
    code: "RPT",
    label: "Spending dashboard",
    description: "See what you spent, by category, over any date range.",
  },
  {
    section: "comparison",
    code: "CMP",
    label: "Spending comparison",
    description: "Compare this month's spend in a category against months past.",
  },
];

/**
 * Default landing view (FE-8): just the app name and one button per section screen
 * (ManageCategories/ReviewTransactions/SpendingDashboard/SpendingComparison from FE-4-FE-7).
 * Upload (FE-3) and the backend connectivity check (FE-2) are intentionally out of this
 * navigation - they're rendered elsewhere in the app shell (see App.tsx).
 *
 * Visual treatment: a "greenbar" continuous-feed statement printout - sprocket-hole
 * margins, a perforated tear line under the masthead, and alternating print bands
 * behind each destination row, styled like line items on a real bank statement
 * (type code + description), in place of plain stacked buttons.
 */
export function LandingPage({ onNavigate }: LandingPageProps) {
  return (
    <div className="mx-auto mt-12 max-w-xl rounded-lg bg-ledger-paper px-4 py-8 sm:px-6">
      <div className="relative grid grid-cols-[1.75rem_1fr_1.75rem] overflow-hidden rounded-sm border border-ledger-ink/15 bg-ledger-white shadow-sm">
        <SprocketMargin side="left" />

        <div className="px-4 py-6 sm:px-6">
          <p className="font-ledger text-xs font-medium tracking-[0.25em] text-ledger-ink">
            STATEMENT
          </p>
          <h1 className="mt-1 font-ledger text-3xl font-bold tracking-tight text-ledger-text sm:text-4xl">
            Bank Categorizer
          </h1>
          <p className="mt-1 font-ledger-body text-sm text-ledger-muted">
            Personal ledger &mdash; single user.
          </p>

          <div className="mt-6 border-t border-dashed border-ledger-ink/30" aria-hidden="true" />

          <div className="mt-2 flex flex-col">
            {SECTION_ROWS.map(({ section, code, label, description }, index) => {
              const descriptionId = `landing-section-desc-${section}`;
              return (
                <button
                  key={section}
                  type="button"
                  onClick={() => onNavigate(section)}
                  aria-label={label}
                  aria-describedby={descriptionId}
                  className={`group grid grid-cols-[3.25rem_1fr_1.5rem] items-start gap-3 border-t border-ledger-ink/10 py-3 text-left first:border-t-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-ledger-stamp focus-visible:ring-offset-2 hover:bg-ledger-ink/5 ${
                    index % 2 === 1 ? "bg-ledger-band/40" : "bg-ledger-white"
                  }`}
                >
                  <span
                    aria-hidden="true"
                    className="pt-0.5 font-ledger text-xs font-semibold tracking-wider text-ledger-ink"
                  >
                    {code}
                  </span>
                  <span>
                    <span className="block font-ledger-body text-sm font-semibold text-ledger-text">
                      {label}
                    </span>
                    <span
                      id={descriptionId}
                      className="mt-0.5 block font-ledger-body text-xs text-ledger-muted"
                    >
                      {description}
                    </span>
                  </span>
                  <span
                    aria-hidden="true"
                    className="pt-0.5 text-sm text-ledger-ink transition-transform motion-safe:duration-150 motion-safe:group-hover:translate-x-1 motion-safe:group-focus-visible:translate-x-1"
                  >
                    &rarr;
                  </span>
                </button>
              );
            })}
          </div>

          <div className="mt-4 border-t border-dashed border-ledger-ink/30 pt-3 text-center">
            <p className="font-ledger text-[10px] uppercase tracking-[0.3em] text-ledger-muted">
              &mdash; end of statement &mdash;
            </p>
          </div>
        </div>

        <SprocketMargin side="right" />
      </div>
    </div>
  );
}

/** Decorative sprocket-hole margin, echoing tractor-feed printout paper. Purely visual. */
function SprocketMargin({ side }: { side: "left" | "right" }) {
  return (
    <div
      aria-hidden="true"
      className={`flex flex-col items-center justify-around bg-ledger-white py-6 ${
        side === "left" ? "border-r border-ledger-ink/10" : "border-l border-ledger-ink/10"
      }`}
    >
      {Array.from({ length: 6 }, (_, i) => (
        <span key={i} className="h-2 w-2 rounded-full bg-ledger-paper" />
      ))}
    </div>
  );
}
