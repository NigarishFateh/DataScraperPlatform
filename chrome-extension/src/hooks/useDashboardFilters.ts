import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import {
  fetchCategoriesForCompanies,
  fetchCities,
  fetchCompanies,
  fetchCountries,
} from "../services/catalog/catalogApi";
import type { DashboardSelection } from "../types/catalog";

/**
 * Cascading filter state for the Dashboard.
 *
 * Ownership of truth:
 * - React Query cache = server (dummy) data
 * - useState = user selection
 * Changing an upstream filter clears downstream selections (cascade).
 */
export function useDashboardFilters() {
  const [countryCode, setCountryCodeState] = useState<string | null>(null);
  const [cityIds, setCityIdsState] = useState<string[]>([]);
  const [companyIds, setCompanyIdsState] = useState<string[]>([]);
  const [categoryIds, setCategoryIdsState] = useState<string[]>([]);
  const [citySearch, setCitySearch] = useState("");
  const [companySearch, setCompanySearch] = useState("");

  const countriesQuery = useQuery({
    queryKey: ["countries"],
    queryFn: fetchCountries,
  });

  const citiesQuery = useQuery({
    queryKey: ["cities", countryCode, citySearch],
    queryFn: () => fetchCities(countryCode!, citySearch),
    enabled: Boolean(countryCode),
  });

  const companiesQuery = useInfiniteQuery({
    queryKey: ["companies", cityIds, companySearch],
    queryFn: ({ pageParam }) =>
      fetchCompanies({
        cityIds,
        search: companySearch,
        page: pageParam,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? lastPage.page + 1 : undefined),
    enabled: cityIds.length > 0,
  });

  const companies = useMemo(
    () => companiesQuery.data?.pages.flatMap((page) => page.items) ?? [],
    [companiesQuery.data],
  );

  const totalCompanies = companiesQuery.data?.pages[0]?.total ?? 0;

  const categoriesQuery = useQuery({
    queryKey: ["categories", companyIds, companies.length],
    queryFn: () => fetchCategoriesForCompanies(companyIds, companies),
    enabled: companyIds.length > 0 && companies.length > 0,
  });

  function setCountryCode(next: string | null) {
    setCountryCodeState(next);
    setCityIdsState([]);
    setCompanyIdsState([]);
    setCategoryIdsState([]);
    setCitySearch("");
    setCompanySearch("");
  }

  function toggleCity(cityId: string) {
    setCityIdsState((prev) => {
      const next = prev.includes(cityId)
        ? prev.filter((id) => id !== cityId)
        : [...prev, cityId];
      return next;
    });
    setCompanyIdsState([]);
    setCategoryIdsState([]);
    setCompanySearch("");
  }

  function toggleCompany(companyId: string) {
    setCompanyIdsState((prev) => {
      const next = prev.includes(companyId)
        ? prev.filter((id) => id !== companyId)
        : [...prev, companyId];
      return next;
    });
    setCategoryIdsState([]);
  }

  function toggleCategory(categoryId: string) {
    setCategoryIdsState((prev) =>
      prev.includes(categoryId)
        ? prev.filter((id) => id !== categoryId)
        : [...prev, categoryId],
    );
  }

  const selection: DashboardSelection = {
    countryCode,
    cityIds,
    companyIds,
    categoryIds,
  };

  const canSearch =
    Boolean(countryCode) &&
    cityIds.length > 0 &&
    companyIds.length > 0 &&
    categoryIds.length > 0;

  return {
    selection,
    canSearch,
    countryCode,
    cityIds,
    companyIds,
    categoryIds,
    citySearch,
    companySearch,
    setCountryCode,
    setCitySearch,
    setCompanySearch,
    toggleCity,
    toggleCompany,
    toggleCategory,
    countriesQuery,
    citiesQuery,
    companiesQuery,
    categoriesQuery,
    companies,
    totalCompanies,
  };
}
