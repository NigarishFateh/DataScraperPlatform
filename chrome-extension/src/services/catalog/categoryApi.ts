import { apiFetch } from "../api/client";
import type { Category } from "../../types/catalog";

type CategoryDto = {
  id: string;
  name: string;
};

function mapCategory(dto: CategoryDto): Category {
  return { id: dto.id, name: dto.name };
}

export async function fetchCategoriesFromApi(ids?: string[]): Promise<Category[]> {
  const params = new URLSearchParams();
  if (ids?.length) {
    ids.forEach((id) => params.append("ids", id));
  }

  const query = params.toString();
  const response = await apiFetch(`/api/categories${query ? `?${query}` : ""}`);
  if (!response.ok) {
    throw new Error(`Failed to load categories (${response.status})`);
  }

  const data = (await response.json()) as CategoryDto[];
  return data.map(mapCategory);
}
