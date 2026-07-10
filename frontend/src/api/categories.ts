import { request } from "./client";
import type { CategoryCreateRequest, CategoryResponse } from "./types";

/** GET /api/v1/categories */
export function listCategories(): Promise<CategoryResponse[]> {
  return request<CategoryResponse[]>("/api/v1/categories");
}

/** POST /api/v1/categories */
export function createCategory(body: CategoryCreateRequest): Promise<CategoryResponse> {
  return request<CategoryResponse>("/api/v1/categories", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/** DELETE /api/v1/categories/{id} */
export function deleteCategory(id: number): Promise<void> {
  return request<void>(`/api/v1/categories/${id}`, { method: "DELETE" });
}
