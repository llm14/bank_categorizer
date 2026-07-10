import { buildQuery, request, requestFormData } from "./client";
import type {
  ImportResultResponse,
  PageResponse,
  TransactionCategoryFilter,
  TransactionResponse,
  TransactionUpdateRequest,
} from "./types";

export interface ListTransactionsParams {
  /** Only "uncategorized" is supported by the backend today. */
  category?: TransactionCategoryFilter;
  /** Zero-indexed page number (backend default: 0). */
  page?: number;
  /** Page size (backend default: 20). */
  size?: number;
  /** e.g. "date,desc" (backend default: date descending). */
  sort?: string;
}

/** GET /api/v1/transactions[?category=uncategorized&page=&size=&sort=] */
export function listTransactions(
  params: ListTransactionsParams = {},
): Promise<PageResponse<TransactionResponse>> {
  const query = buildQuery({
    category: params.category,
    page: params.page,
    size: params.size,
    sort: params.sort,
  });
  return request<PageResponse<TransactionResponse>>(`/api/v1/transactions${query}`);
}

/** PATCH /api/v1/transactions/{id} */
export function updateTransactionCategory(
  id: number,
  body: TransactionUpdateRequest,
): Promise<TransactionResponse> {
  return request<TransactionResponse>(`/api/v1/transactions/${id}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

/** POST /api/v1/transactions/import (multipart/form-data, field name "file"). */
export function importTransactions(file: File): Promise<ImportResultResponse> {
  const formData = new FormData();
  formData.set("file", file);
  return requestFormData<ImportResultResponse>("/api/v1/transactions/import", formData);
}
