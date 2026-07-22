export type Country = {
  code: string;
  name: string;
};

export type City = {
  id: string;
  name: string;
  countryCode: string;
};

export type Company = {
  id: string;
  name: string;
  website: string;
  industry: string;
  cityId: string;
  countryCode: string;
  categoryIds: string[];
};

export type Category = {
  id: string;
  name: string;
};

export type CompanyPage = {
  items: Company[];
  page: number;
  pageSize: number;
  total: number;
  hasMore: boolean;
};

export type DashboardSelection = {
  countryCode: string | null;
  cityIds: string[];
  companyIds: string[];
  categoryIds: string[];
};
