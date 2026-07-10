import { buildQuery, request } from "./client";
import type { SpendingComparisonResponse, SpendingResponse } from "./types";

export interface SpendingForCategoryParams {
  category: number;
  from?: string;
  to?: string;
}

/** GET /api/v1/spending?category={id}&from=&to= */
export function getSpendingForCategory(
  params: SpendingForCategoryParams,
): Promise<SpendingResponse> {
  const query = buildQuery({ category: params.category, from: params.from, to: params.to });
  return request<SpendingResponse>(`/api/v1/spending${query}`);
}

export interface SpendingBreakdownParams {
  from?: string;
  to?: string;
}

/** GET /api/v1/spending?from=&to= (no category -> breakdown across all categories). */
export function getSpendingBreakdown(
  params: SpendingBreakdownParams = {},
): Promise<SpendingResponse[]> {
  const query = buildQuery({ from: params.from, to: params.to });
  return request<SpendingResponse[]>(`/api/v1/spending${query}`);
}

export interface CompareSpendingParams {
  category: number;
  /** Only "month" is supported by the backend today. */
  period?: string;
  /** Bounded 1-24 by the backend. */
  lookback?: number;
}

/** GET /api/v1/spending/compare?category={id}&period=month&lookback={n} */
export function compareSpending(
  params: CompareSpendingParams,
): Promise<SpendingComparisonResponse> {
  const query = buildQuery({
    category: params.category,
    period: params.period,
    lookback: params.lookback,
  });
  return request<SpendingComparisonResponse>(`/api/v1/spending/compare${query}`);
}
