import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { UploadStatement } from "./UploadStatement";
import { importTransactions } from "../api/transactions";
import { ApiError } from "../api/client";

vi.mock("../api/transactions");

const mockedImportTransactions = vi.mocked(importTransactions);

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <UploadStatement />
    </QueryClientProvider>,
  );
}

function makeFile(name = "transactions_basic.csv") {
  return new File(["date,description,amount\n"], name, { type: "text/csv" });
}

describe("UploadStatement", () => {
  it("uploads the selected file and shows the import summary on success", async () => {
    mockedImportTransactions.mockResolvedValue({
      fileName: "transactions_basic.csv",
      totalRows: 5,
      importedCount: 5,
      skippedCount: 0,
      categorizedCount: 3,
      uncategorizedCount: 2,
    });

    renderWithClient();
    const user = userEvent.setup();

    await user.upload(screen.getByLabelText(/bank statement file/i), makeFile());
    await user.click(screen.getByRole("button", { name: /upload/i }));

    expect(
      await screen.findByText(/imported 5 of 5 rows — 3 categorized, 2 uncategorized\./i),
    ).toBeInTheDocument();
    expect(mockedImportTransactions).toHaveBeenCalledWith(expect.objectContaining({ name: "transactions_basic.csv" }));
  });

  it("mentions skipped rows when some rows were skipped", async () => {
    mockedImportTransactions.mockResolvedValue({
      fileName: "transactions_basic.csv",
      totalRows: 5,
      importedCount: 4,
      skippedCount: 1,
      categorizedCount: 3,
      uncategorizedCount: 1,
    });

    renderWithClient();
    const user = userEvent.setup();

    await user.upload(screen.getByLabelText(/bank statement file/i), makeFile());
    await user.click(screen.getByRole("button", { name: /upload/i }));

    expect(await screen.findByText(/\(1 skipped\)/i)).toBeInTheDocument();
  });

  it("shows the backend's actual message for an unsupported file type (400)", async () => {
    mockedImportTransactions.mockRejectedValue(
      new ApiError(400, {
        timestamp: "2026-07-10T00:00:00Z",
        status: 400,
        error: "Bad Request",
        message: "Unsupported file type: .pdf. Only CSV and XLSX files are supported.",
      }),
    );

    renderWithClient();
    // The input's `accept` attribute is a native-picker hint, not a hard restriction - a real
    // user can still select a file with an unsupported extension (e.g. via "All Files"), so
    // disable user-event's accept-based filtering to exercise that path.
    const user = userEvent.setup({ applyAccept: false });

    await user.upload(screen.getByLabelText(/bank statement file/i), makeFile("statement.pdf"));
    await user.click(screen.getByRole("button", { name: /upload/i }));

    expect(
      await screen.findByText(/unsupported file type: \.pdf\. only csv and xlsx files are supported\./i),
    ).toBeInTheDocument();
  });

  it("shows the backend's actual message for an oversized file (413)", async () => {
    mockedImportTransactions.mockRejectedValue(
      new ApiError(413, {
        timestamp: "2026-07-10T00:00:00Z",
        status: 413,
        error: "Payload Too Large",
        message: "File size exceeds the maximum allowed size of 15MB.",
      }),
    );

    renderWithClient();
    const user = userEvent.setup();

    await user.upload(screen.getByLabelText(/bank statement file/i), makeFile("huge.csv"));
    await user.click(screen.getByRole("button", { name: /upload/i }));

    expect(
      await screen.findByText(/file size exceeds the maximum allowed size of 15mb\./i),
    ).toBeInTheDocument();
  });

  it("disables the upload button until a file is selected", () => {
    renderWithClient();

    expect(screen.getByRole("button", { name: /upload/i })).toBeDisabled();
  });
});
