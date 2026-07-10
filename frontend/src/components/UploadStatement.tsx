import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { importTransactions } from "../api/transactions";
import { ApiError } from "../api/client";

/**
 * Upload screen (FE-3): lets the user pick a CSV/XLSX bank statement and posts it to
 * POST /api/v1/transactions/import. On success it renders the ImportResultResponse summary;
 * on failure (e.g. 400 unsupported file type, 413 oversized file) it surfaces the backend's
 * actual error message rather than a generic failure string.
 */
export function UploadStatement() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const mutation = useMutation({
    mutationFn: (file: File) => importTransactions(file),
  });

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    setSelectedFile(event.target.files?.[0] ?? null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFile) {
      return;
    }
    mutation.mutate(selectedFile);
  }

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.error instanceof Error
        ? mutation.error.message
        : null;

  return (
    <div className="mx-auto mt-8 max-w-md rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-semibold text-gray-900">Upload bank statement</h2>
      <p className="mt-1 text-sm text-gray-500">Import a CSV or XLSX statement to categorize its transactions.</p>

      <form className="mt-4" onSubmit={handleSubmit}>
        <input
          type="file"
          accept=".csv,.xlsx"
          aria-label="Bank statement file"
          onChange={handleFileChange}
          className="block w-full text-sm text-gray-700"
        />

        <button
          type="submit"
          disabled={!selectedFile || mutation.isPending}
          className="mt-4 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300"
        >
          {mutation.isPending ? "Uploading..." : "Upload"}
        </button>
      </form>

      <div className="mt-4" role="status">
        {mutation.isPending && <p className="text-gray-600">Uploading...</p>}

        {mutation.isError && (
          <p className="text-red-700">{errorMessage}</p>
        )}

        {mutation.isSuccess && (
          <p className="text-green-700">
            Imported {mutation.data.importedCount} of {mutation.data.totalRows} rows —{" "}
            {mutation.data.categorizedCount} categorized, {mutation.data.uncategorizedCount} uncategorized
            {mutation.data.skippedCount > 0 ? ` (${mutation.data.skippedCount} skipped)` : ""}.
          </p>
        )}
      </div>
    </div>
  );
}
