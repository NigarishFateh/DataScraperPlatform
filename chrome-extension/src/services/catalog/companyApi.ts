import { apiFetch } from "../api/client";
import type { Company, CompanyPage } from "../../types/catalog";
import type { CompanyQuery } from "./catalogApi";

type CompanyDto = {
  id: string;
  name: string;
  website: string;
  industry: string;
  cityId: string;
  countryCode: string;
  categoryIds: string[];
};

type CompanyPageDto = {
  items: CompanyDto[];
  page: number;
  pageSize: number;
  total: number;
  hasMore: boolean;
};

function mapCompany(dto: CompanyDto): Company {
  return {
    id: dto.id,
    name: dto.name,
    website: dto.website,
    industry: dto.industry,
    cityId: dto.cityId,
    countryCode: dto.countryCode,
    categoryIds: dto.categoryIds,
  };
}

export async function fetchCompaniesFromApi(query: CompanyQuery): Promise<CompanyPage> {
  const params = new URLSearchParams();
  query.cityIds.forEach((id) => params.append("cityIds", id));
  if (query.search?.trim()) {
    params.set("search", query.search.trim());
  }
  params.set("page", String(query.page));
  params.set("pageSize", String(query.pageSize ?? 8));

  const response = await apiFetch(`/api/companies/search?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`Failed to load companies (${response.status})`);
  }

  const data = (await response.json()) as CompanyPageDto;
  return {
    items: data.items.map(mapCompany),
    page: data.page,
    pageSize: data.pageSize,
    total: data.total,
    hasMore: data.hasMore,
  };
}
