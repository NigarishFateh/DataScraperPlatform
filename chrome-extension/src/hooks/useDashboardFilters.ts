import { useEffect, useState } from "react";
import { fetchDefaultCategory } from "../services/catalog/catalogApi";
import { getUserSettings } from "../services/storage/settingsStorage";
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

export function useDashboardFilters() {
  const [countryCodes, setCountryCodes] = useState<string[]>([]);
  const [cityIds, setCityIds] = useState<string[]>([]);
  const [categoryIds, setCategoryIds] = useState<string[]>([]);
  const [maxCompanies, setMaxCompanies] = useState(200);
  const [defaultLoaded, setDefaultLoaded] = useState(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      const pending = readPendingFilters();
      if (pending) {
        if (!cancelled) {
          setCountryCodes(pending.countryCodes);
          setCityIds(pending.cityIds);
          setCategoryIds(pending.categoryIds);
          setMaxCompanies(pending.maxCompanies);
          setDefaultLoaded(true);
        }
        return;
      }

      try {
        const [settings, defaultCategory] = await Promise.all([
          getUserSettings(),
          fetchDefaultCategory(),
        ]);
        if (cancelled) return;
        setMaxCompanies(settings.defaultMaxCompanies);
        setCategoryIds([defaultCategory.id]);
      } catch {
        // Default category is best-effort; user can still pick manually.
      } finally {
        if (!cancelled) {
          setDefaultLoaded(true);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  function toggleCountry(countryCode: string) {
    setCountryCodes((prev) =>
      prev.includes(countryCode)
        ? prev.filter((code) => code !== countryCode)
        : [...prev, countryCode],
    );
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
    maxCompanies,
  };

  const canStart = categoryIds.length > 0 && defaultLoaded;

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
