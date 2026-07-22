import type { Category, City, CompanyPage, Country } from "../../types/catalog";
import { fetchCategoriesFromApi } from "./categoryApi";
import { fetchCompaniesFromApi } from "./companyApi";
import { fetchCitiesFromApi, fetchCountriesFromApi } from "./locationApi";
import { CATEGORIES, CITIES, COMPANIES, COUNTRIES } from "../../data/dummyCatalog";

const USE_BACKEND_LOCATIONS =
  (import.meta.env.VITE_LOCATION_SOURCE ?? "backend") === "backend";
const USE_BACKEND_COMPANIES =
  (import.meta.env.VITE_COMPANY_SOURCE ?? "backend") === "backend";
const USE_BACKEND_CATEGORIES =
  (import.meta.env.VITE_CATEGORY_SOURCE ?? "backend") === "backend";

/** Artificial latency for dummy company/category paths only. */
function delay(ms = 280): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export async function fetchCountries(): Promise<Country[]> {
  if (USE_BACKEND_LOCATIONS) {
    return fetchCountriesFromApi();
  }
  await delay(150);
  return COUNTRIES;
}

export async function fetchCities(countryCode: string, search = ""): Promise<City[]> {
  if (USE_BACKEND_LOCATIONS) {
    return fetchCitiesFromApi(countryCode, search);
  }
  await delay();
  const q = search.trim().toLowerCase();
  return CITIES.filter((city) => {
    if (city.countryCode !== countryCode) return false;
    if (!q) return true;
    return city.name.toLowerCase().includes(q);
  });
}

export type CompanyQuery = {
  cityIds: string[];
  search?: string;
  page: number;
  pageSize?: number;
};

export async function fetchCompanies(query: CompanyQuery): Promise<CompanyPage> {
  if (USE_BACKEND_COMPANIES) {
    return fetchCompaniesFromApi(query);
  }

  await delay(350);
  const pageSize = query.pageSize ?? 8;
  const q = (query.search ?? "").trim().toLowerCase();

  const filtered = COMPANIES.filter((company) => {
    if (!query.cityIds.includes(company.cityId)) return false;
    if (!q) return true;
    return (
      company.name.toLowerCase().includes(q) ||
      company.website.toLowerCase().includes(q) ||
      company.industry.toLowerCase().includes(q)
    );
  });

  const start = query.page * pageSize;
  const items = filtered.slice(start, start + pageSize);
  return {
    items,
    page: query.page,
    pageSize,
    total: filtered.length,
    hasMore: start + pageSize < filtered.length,
  };
}

export async function fetchCategoriesForCompanies(
  companyIds: string[],
  sourceCompanies: typeof COMPANIES = COMPANIES,
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

  if (USE_BACKEND_CATEGORIES) {
    return fetchCategoriesFromApi([...categoryIds]);
  }

  await delay(200);
  return CATEGORIES.filter((category) => categoryIds.has(category.id));
}
