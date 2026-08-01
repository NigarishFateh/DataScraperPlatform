export type Country = {
  code: string;
  name: string;
};

export type City = {
  id: string;
  name: string;
  countryCode: string;
};

export type Category = {
  id: string;
  name: string;
};

export type DashboardFilters = {
  countryCodes: string[];
  cityIds: string[];
  categoryIds: string[];
  maxCompanies: number;
};
