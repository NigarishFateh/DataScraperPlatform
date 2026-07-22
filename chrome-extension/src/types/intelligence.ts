export type IntelligenceJobStatus = "COMPLETED" | "PARTIAL" | "FAILED";

export type ScraperExecutionStatus = "SUCCESS" | "FAILED" | "SKIPPED";

export type ScraperType =
  | "COMPANY_WEBSITE"
  | "TECHNOLOGY_STACK"
  | "NEWS"
  | "GITHUB"
  | "CONTACT";

export type ScraperResult = {
  scraperType: ScraperType;
  status: ScraperExecutionStatus;
  message: string;
  scrapedAt: string;
  items: Record<string, unknown>[];
  metadata: Record<string, unknown>;
};

export type IntelligenceJobResponse = {
  jobId: string;
  status: IntelligenceJobStatus;
  message: string;
  elapsedMs: number;
  results: ScraperResult[];
};

export type IntelligenceJobRequest = {
  companyId: string;
  companyName: string;
  websiteUrl: string;
  categoryIds: string[];
};
