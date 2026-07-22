import type { Category, City, Company, CompanyPage, Country } from "../../types/catalog";
import { fetchCategoriesFromApi } from "./categoryApi";
import { fetchCompaniesFromApi } from "./companyApi";
import { fetchCitiesFromApi, fetchCountriesFromApi } from "./locationApi";

export async function fetchCountries(): Promise<Country[]> {
  return fetchCountriesFromApi();
}

export async function fetchCities(countryCode: string, search = ""): Promise<City[]> {
  return fetchCitiesFromApi(countryCode, search);
}

export type CompanyQuery = {
  cityIds: string[];
  search?: string;
  page: number;
  pageSize?: number;
};

export async function fetchCompanies(query: CompanyQuery): Promise<CompanyPage> {
  return fetchCompaniesFromApi(query);
}

export async function fetchCategoriesForCompanies(
  companyIds: string[],
  sourceCompanies: Company[],
): Promise<Category[]> {
  if (companyIds.length === 0) return [];

  const idSet = new Set(companyIds);
  const categoryIds = new Set<string>();
  for (const company of sourceCompanies) {
    if (!idSet.has(company.id)) continue;
    for (const categoryId of company.categoryIds) {
      categoryIds.add(categoryId);
    }
  }

  return fetchCategoriesFromApi([...categoryIds]);
}
