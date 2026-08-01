import { apiFetch } from "../api/client";
import type { PageResponse } from "../../types/common";
import type { Category } from "../../types/catalog";

type CategoryDto = {
  id: string;
  name: string;
};

function mapCategory(dto: CategoryDto): Category {
  return { id: dto.id, name: dto.name };
}

export type CategoryQuery = {
  search?: string;
  page?: number;
  pageSize?: number;
  ids?: string[];
};

export async function fetchCategoriesPage(
  query: CategoryQuery = {},
): Promise<PageResponse<Category>> {
  const params = new URLSearchParams();
  if (query.search?.trim()) {
    params.set("search", query.search.trim());
  }
  params.set("page", String(query.page ?? 0));
  params.set("pageSize", String(query.pageSize ?? 50));
  query.ids?.forEach((id) => params.append("ids", id));

  const queryString = params.toString();
  const response = await apiFetch(`/api/categories${queryString ? `?${queryString}` : ""}`);
  if (!response.ok) {
    throw new Error(`Failed to load categories (${response.status})`);
  }

  const data = (await response.json()) as PageResponse<CategoryDto>;
  return {
    ...data,
    items: data.items.map(mapCategory),
  };
}

export async function fetchDefaultCategory(): Promise<Category> {
  const response = await apiFetch("/api/categories/default");
  if (!response.ok) {
    throw new Error(`Failed to load default category (${response.status})`);
  }
  const data = (await response.json()) as CategoryDto;
  return mapCategory(data);
}
