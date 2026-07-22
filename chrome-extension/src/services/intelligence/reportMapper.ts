import type { ScraperResult, ScraperType } from "../../types/intelligence";

const SECTION_BY_SCRAPER: Record<ScraperType, string> = {
  COMPANY_WEBSITE: "identity",
  TECHNOLOGY_STACK: "technology",
  NEWS: "presence",
  GITHUB: "presence",
  CONTACT: "contact",
};

export function groupResultsBySection(
  results: ScraperResult[],
): Record<string, ScraperResult[]> {
  const grouped: Record<string, ScraperResult[]> = {
    identity: [],
    positioning: [],
    offerings: [],
    technology: [],
    presence: [],
    contact: [],
  };

  for (const result of results) {
    const section = SECTION_BY_SCRAPER[result.scraperType] ?? "identity";
    grouped[section].push(result);

    if (result.scraperType === "COMPANY_WEBSITE" && result.status === "SUCCESS") {
      grouped.positioning.push(result);
      grouped.offerings.push(result);
    }
  }

  return grouped;
}

export function formatResultItems(result: ScraperResult): string[] {
  if (!result.items.length) {
    return [result.message];
  }

  return result.items.flatMap((item) =>
    Object.entries(item).map(([key, value]) => `${key}: ${String(value)}`),
  );
}
