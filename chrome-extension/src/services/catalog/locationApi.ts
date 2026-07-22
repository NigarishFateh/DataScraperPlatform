import { apiFetch } from "../api/client";
import type { City, Country } from "../../types/catalog";

type CountryDto = { code: string; name: string };
type CityDto = { id: string; name: string; countryCode: string };

export async function fetchCountriesFromApi(): Promise<Country[]> {
  const response = await apiFetch("/api/locations/countries");
  if (!response.ok) {
    throw new Error(`Failed to load countries (${response.status})`);
  }
  const data = (await response.json()) as CountryDto[];
  return data.map((country) => ({ code: country.code, name: country.name }));
}

export async function fetchCitiesFromApi(countryCode: string, search = ""): Promise<City[]> {
  const params = new URLSearchParams({ countryCode });
  if (search.trim()) {
    params.set("search", search.trim());
  }
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
