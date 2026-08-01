import type { CategoryQuery } from "./categoryApi";
import { fetchCategoriesPage, fetchDefaultCategory } from "./categoryApi";
import type { LocationQuery } from "./locationApi";
import { fetchCities, fetchCountriesPage } from "./locationApi";

export async function fetchCountries(query: LocationQuery = {}) {
  return fetchCountriesPage(query);
}

export { fetchCities, fetchCountriesPage, fetchCategoriesPage, fetchDefaultCategory };
export type { LocationQuery, CategoryQuery };
