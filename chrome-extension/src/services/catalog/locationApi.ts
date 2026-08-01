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

async function fetchCitiesForCountry(search: string, countryCode: string): Promise<City[]> {
  const params = new URLSearchParams();
  if (search.trim()) {
    params.set("search", search.trim());
  }
  params.set("countryCode", countryCode.trim());

  const response = await apiFetch(`/api/locations/cities?${params.toString()}`);
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

/**
 * Loads cities for one or more countries. Returns [] when no country is selected.
 */
export async function fetchCities(search = "", countryCodes: string[] = []): Promise<City[]> {
  const codes = countryCodes.map((code) => code.trim().toUpperCase()).filter(Boolean);
  if (codes.length === 0) {
    return [];
  }

  const batches = await Promise.all(codes.map((code) => fetchCitiesForCountry(search, code)));
  const byId = new Map<string, City>();
  for (const batch of batches) {
    for (const city of batch) {
      byId.set(city.id, city);
    }
  }
  return [...byId.values()].sort((a, b) => a.name.localeCompare(b.name));
}
