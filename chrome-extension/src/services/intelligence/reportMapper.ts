import type { ScraperResult, ScraperType } from "../../types/intelligence";

export type ReportSectionId =
  | "identity"
  | "positioning"
  | "offerings"
  | "technology"
  | "presence"
  | "contact";

export type ReportDisplayItem = {
  id: string;
  label: string;
  value: string;
  href?: string;
  note?: string;
};

export type ReportDisplayValue = {
  id: string;
  value: string;
  href?: string;
  note?: string;
};

/** One label with one or more values (e.g. all phones under "Phone numbers"). */
export type ReportDisplayGroup = {
  id: string;
  label: string;
  values: ReportDisplayValue[];
};

export type ReportSectionView = {
  statusLabel: string | null;
  items: ReportDisplayItem[];
  groups: ReportDisplayGroup[];
  emptyMessage: string;
};

const SECTION_BY_SCRAPER: Record<ScraperType, ReportSectionId> = {
  COMPANY_WEBSITE: "identity",
  TECHNOLOGY_STACK: "technology",
  NEWS: "presence",
  GITHUB: "presence",
  CONTACT: "contact",
};

const FIELD_LABELS: Record<string, string> = {
  pageTitle: "Page title",
  metaDescription: "Description",
  ogTitle: "Open Graph title",
  ogDescription: "Open Graph description",
  ogSiteName: "Site name",
  canonicalUrl: "Canonical URL",
  heading: "Heading",
  paragraph: "About",
  "service-or-product-link": "Service / product",
  "careers-link": "Careers",
  linkedin: "LinkedIn",
  github: "GitHub",
  twitter: "X / Twitter",
  facebook: "Facebook",
  youtube: "YouTube",
  email: "Email",
  phone: "Phone",
  address: "Address",
  technology: "Detected technology",
  "meta-generator": "Generator",
  "script-src": "Script signal",
  "news-headline": "News",
  "github-organization": "GitHub organization",
};

const WEBSITE_SECTION_FIELDS: Record<ReportSectionId, Set<string>> = {
  identity: new Set([
    "pageTitle",
    "metaDescription",
    "ogTitle",
    "ogDescription",
    "ogSiteName",
    "canonicalUrl",
  ]),
  positioning: new Set(["heading", "paragraph"]),
  offerings: new Set(["service-or-product-link"]),
  technology: new Set(),
  presence: new Set(["linkedin", "github", "twitter", "facebook", "youtube", "careers-link"]),
  contact: new Set(["email", "phone", "address"]),
};

function asString(value: unknown): string {
  if (value == null) return "";
  return String(value).trim();
}

function fieldLabel(field: string): string {
  return FIELD_LABELS[field] ?? field.replace(/[-_]/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
}

function itemSection(item: Record<string, unknown>): string {
  return asString(item.section).toLowerCase();
}

function itemField(item: Record<string, unknown>): string {
  return asString(item.field);
}

function itemValue(item: Record<string, unknown>): string {
  return (
    asString(item.title) ||
    asString(item.value) ||
    asString(item.login) ||
    asString(item.description) ||
    asString(item.profileUrl) ||
    asString(item.url)
  );
}

function itemHref(item: Record<string, unknown>): string | undefined {
  const field = itemField(item);
  const value = itemValue(item);

  if (field === "email" && value.includes("@")) {
    return `mailto:${value.split("?")[0]}`;
  }
  if (field === "phone" && value) {
    const digits = value.replace(/[^\d+]/g, "");
    if (digits.length >= 7) return `tel:${digits}`;
  }

  const href =
    asString(item.url) ||
    asString(item.profileUrl) ||
    asString(item.href) ||
    asString(item.canonicalUrl);
  if (!href) return undefined;
  if (
    href.startsWith("http://") ||
    href.startsWith("https://") ||
    href.startsWith("mailto:") ||
    href.startsWith("tel:")
  ) {
    return href;
  }
  return undefined;
}

function belongsToReportSection(
  sectionId: ReportSectionId,
  result: ScraperResult,
  item: Record<string, unknown>,
): boolean {
  const field = itemField(item);
  const scrapedSection = itemSection(item);

  if (result.scraperType === "COMPANY_WEBSITE") {
    if (WEBSITE_SECTION_FIELDS[sectionId].has(field)) return true;
    if (sectionId === "offerings" && scrapedSection === "offerings") return true;
    if (sectionId === "positioning" && scrapedSection === "positioning") return true;
    if (sectionId === "identity" && scrapedSection === "identity") return true;
    if (sectionId === "presence" && (scrapedSection === "presence" || scrapedSection === "talent")) return true;
    if (sectionId === "contact" && scrapedSection === "contact") return true;
    return false;
  }

  if (result.scraperType === "CONTACT") {
    return sectionId === "contact";
  }

  if (result.scraperType === "TECHNOLOGY_STACK") {
    return sectionId === "technology";
  }

  if (result.scraperType === "NEWS" || result.scraperType === "GITHUB") {
    return sectionId === "presence";
  }

  return SECTION_BY_SCRAPER[result.scraperType] === sectionId;
}

function looksLikeUrl(value: string): boolean {
  const v = value.trim();
  return /^(https?:\/\/|www\.)/i.test(v) || /^[a-z0-9.-]+\.[a-z]{2,}(\/|$)/i.test(v);
}

function groupLabel(label: string, count: number): string {
  if (count <= 1) return label;
  const plural: Record<string, string> = {
    Phone: "Phone numbers",
    Email: "Email addresses",
    Address: "Addresses",
    Heading: "Headings",
    About: "About",
    News: "News",
    "Service / product": "Services & products",
    "Detected technology": "Detected technologies",
  };
  return plural[label] ?? `${label}s`;
}

function toDisplayItem(
  result: ScraperResult,
  item: Record<string, unknown>,
  index: number,
): ReportDisplayItem | null {
  const field = itemField(item);
  const value = itemValue(item);
  if (!value) return null;

  // Headings that are just URLs duplicate the company website — skip them.
  if (field === "heading" && looksLikeUrl(value)) return null;

  const href = itemHref(item);
  const noteParts: string[] = [];
  const tag = asString(item.tag);
  const publishedAt = asString(item.publishedAt);
  const source = asString(item.source);
  if (tag) noteParts.push(tag.toUpperCase());
  if (publishedAt) noteParts.push(publishedAt);
  if (source) noteParts.push(source);

  return {
    id: `${result.scraperType}-${field}-${index}-${value.slice(0, 24)}`,
    label: fieldLabel(field || result.scraperType.toLowerCase()),
    value,
    href: href && href !== value ? href : href,
    note: noteParts.length ? noteParts.join(" · ") : undefined,
  };
}

function dedupeItems(items: ReportDisplayItem[]): ReportDisplayItem[] {
  const seen = new Set<string>();
  const out: ReportDisplayItem[] = [];
  for (const item of items) {
    const key = `${item.label}|${item.value}|${item.href ?? ""}`.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(item);
  }
  return out;
}

function groupItems(items: ReportDisplayItem[]): ReportDisplayGroup[] {
  const order: string[] = [];
  const byLabel = new Map<string, ReportDisplayValue[]>();

  for (const item of items) {
    if (!byLabel.has(item.label)) {
      byLabel.set(item.label, []);
      order.push(item.label);
    }
    byLabel.get(item.label)!.push({
      id: item.id,
      value: item.value,
      href: item.href,
      note: item.note,
    });
  }

  return order.map((label) => {
    const values = byLabel.get(label)!;
    return {
      id: `group-${label}`,
      label: groupLabel(label, values.length),
      values,
    };
  });
}

function buildStatusLabel(
  statuses: { name: string; ok: boolean; cached: boolean }[],
): string | null {
  if (statuses.length === 0) return null;

  const failed = statuses.filter((s) => !s.ok);
  if (failed.length > 0) {
    return failed.map((s) => `${s.name} unavailable`).join(" · ");
  }

  const names = [...new Set(statuses.map((s) => s.name))];
  const cached = statuses.some((s) => s.cached);
  if (names.length === 1) {
    return cached ? `From ${names[0]} (cached)` : `From ${names[0]}`;
  }
  const list =
    names.length === 2 ? `${names[0]} and ${names[1]}` : `${names.slice(0, -1).join(", ")}, and ${names[names.length - 1]}`;
  return cached ? `From ${list} (cached)` : `From ${list}`;
}

export function buildSectionView(
  sectionId: ReportSectionId,
  results: ScraperResult[],
): ReportSectionView {
  const relevant = results.filter((result) => {
    if (result.scraperType === "COMPANY_WEBSITE") {
      return ["identity", "positioning", "offerings", "presence", "contact"].includes(sectionId);
    }
    return SECTION_BY_SCRAPER[result.scraperType] === sectionId;
  });

  const items: ReportDisplayItem[] = [];
  const statuses: { name: string; ok: boolean; cached: boolean }[] = [];
  const notes: string[] = [];

  for (const result of relevant) {
    const name = friendlyScraperName(result.scraperType);
    const cached = Boolean(result.metadata?.fromCache);

    if (result.status !== "SUCCESS") {
      statuses.push({ name, ok: false, cached });
      if (result.message) notes.push(result.message);
      continue;
    }

    statuses.push({ name, ok: true, cached });

    result.items.forEach((raw, index) => {
      if (!belongsToReportSection(sectionId, result, raw)) return;
      const display = toDisplayItem(result, raw, index);
      if (display) items.push(display);
    });

    if (result.items.length === 0 && result.message) {
      notes.push(result.message);
    }
  }

  const unique = dedupeItems(items);
  const groups = groupItems(unique);

  let emptyMessage = "No scraper data for this section yet.";
  if (relevant.length > 0) {
    if (notes.length > 0) {
      emptyMessage = [...new Set(notes)].join(" ");
    } else if (sectionId === "contact") {
      emptyMessage = "No public emails, phones, or addresses found.";
    } else {
      emptyMessage = "No structured findings mapped to this section.";
    }
  }

  return {
    statusLabel: buildStatusLabel(statuses),
    items: unique,
    groups,
    emptyMessage,
  };
}

function friendlyScraperName(type: ScraperType): string {
  switch (type) {
    case "COMPANY_WEBSITE":
      return "Website";
    case "TECHNOLOGY_STACK":
      return "Tech stack";
    case "NEWS":
      return "News";
    case "GITHUB":
      return "GitHub";
    case "CONTACT":
      return "Contact";
    default:
      return type;
  }
}
