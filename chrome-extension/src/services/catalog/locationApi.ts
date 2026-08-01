import { apiFetch } from "../api/client";
import type { PageResponse } from "../../types/common";
import type { City, Country } from "../../types/catalog";

type CountryDto = { code: string; name: string };
type CityDto = { id: string; name: string; countryCode: string };

export type LocationQuery = {
  search?: string;
  page?: number;
  pageSize?: number;
};

export async function fetchCountriesPage(
  query: LocationQuery = {},
): Promise<PageResponse<Country>> {
  const params = new URLSearchParams();
  if (query.search?.trim()) {
    params.set("search", query.search.trim());
  }
  params.set("page", String(query.page ?? 0));
  params.set("pageSize", String(query.pageSize ?? 50));

  const response = await apiFetch(`/api/locations/countries?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`Failed to load countries (${response.status})`);
  }

  const data = (await response.json()) as PageResponse<CountryDto>;
  return {
    ...data,
    items: data.items.map((country) => ({ code: country.code, name: country.name })),
  };
}

export async function fetchCities(search = "", countryCode?: string): Promise<City[]> {
  const params = new URLSearchParams();
  if (search.trim()) {
    params.set("search", search.trim());
  }
  if (countryCode?.trim()) {
    params.set("countryCode", countryCode.trim());
  }

  const query = params.toString();
  const response = await apiFetch(`/api/locations/cities${query ? `?${query}` : ""}`);
  if (!response.ok) {
    throw new Error(`Failed to load cities (${response.status})`);
  }

  const data = (await response.json()) as CityDto[];
  return data.map((city) => ({
    id: city.id,
    name: city.name,
    countryCode: city.countryCode,
  }));
}
