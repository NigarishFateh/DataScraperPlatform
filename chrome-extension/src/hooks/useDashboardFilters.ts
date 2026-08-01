import { useEffect, useState } from "react";
import type { DashboardFilters } from "../types/catalog";

export const PENDING_FILTERS_KEY = "gbi.pendingFilters";

function readPendingFilters(): DashboardFilters | null {
  try {
    const raw = sessionStorage.getItem(PENDING_FILTERS_KEY);
    if (!raw) return null;
    sessionStorage.removeItem(PENDING_FILTERS_KEY);
    return JSON.parse(raw) as DashboardFilters;
  } catch {
    return null;
  }
}

export function stashPendingFilters(filters: DashboardFilters): void {
  sessionStorage.setItem(PENDING_FILTERS_KEY, JSON.stringify(filters));
}

function cityBelongsToCountry(cityId: string, countryCode: string): boolean {
  return cityId.toLowerCase().startsWith(`${countryCode.toLowerCase()}-`);
}

function normalizeMaxCompanies(value: unknown): number | null {
  if (value == null || value === "") return null;
  const n = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(n) || n <= 0) return null;
  return Math.min(5000, Math.floor(n));
}

export function useDashboardFilters() {
  const [countryCodes, setCountryCodes] = useState<string[]>([]);
  const [cityIds, setCityIds] = useState<string[]>([]);
  const [categoryIds, setCategoryIds] = useState<string[]>([]);
  const [maxCompanies, setMaxCompaniesState] = useState<number | null>(null);
  const [defaultLoaded, setDefaultLoaded] = useState(false);

  useEffect(() => {
    const pending = readPendingFilters();
    if (pending) {
      setCountryCodes(pending.countryCodes);
      setCityIds(pending.cityIds);
      setCategoryIds(pending.categoryIds);
      setMaxCompaniesState(normalizeMaxCompanies(pending.maxCompanies));
    }
    setDefaultLoaded(true);
  }, []);

  function setMaxCompanies(value: number | null) {
    setMaxCompaniesState(normalizeMaxCompanies(value));
  }

  function toggleCountry(countryCode: string) {
    setCountryCodes((prev) => {
      const next = prev.includes(countryCode)
        ? prev.filter((code) => code !== countryCode)
        : [...prev, countryCode];

      setCityIds((cities) =>
        cities.filter((cityId) =>
          next.some((code) => cityBelongsToCountry(cityId, code)),
        ),
      );
      return next;
    });
  }

  function toggleCity(cityId: string) {
    setCityIds((prev) =>
      prev.includes(cityId) ? prev.filter((id) => id !== cityId) : [...prev, cityId],
    );
  }

  function toggleCategory(categoryId: string) {
    setCategoryIds((prev) =>
      prev.includes(categoryId)
        ? prev.filter((id) => id !== categoryId)
        : [...prev, categoryId],
    );
  }

  const filters: DashboardFilters = {
    countryCodes,
    cityIds,
    categoryIds,
    maxCompanies: maxCompanies ?? 0,
  };

  const canStart =
    categoryIds.length > 0 && maxCompanies != null && maxCompanies > 0 && defaultLoaded;

  return {
    filters,
    canStart,
    defaultLoaded,
    countryCodes,
    cityIds,
    categoryIds,
    maxCompanies,
    setMaxCompanies,
    toggleCountry,
    toggleCity,
    toggleCategory,
  };
}
