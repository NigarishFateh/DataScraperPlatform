import type { Category, City, Company, Country } from "../types/catalog";

/** Europe-first country list (Phase 5 dummy / Phase 6+ Location Service). */
export const COUNTRIES: Country[] = [
  { code: "DE", name: "Germany" },
  { code: "NL", name: "Netherlands" },
  { code: "BE", name: "Belgium" },
  { code: "FR", name: "France" },
  { code: "ES", name: "Spain" },
  { code: "IT", name: "Italy" },
  { code: "PT", name: "Portugal" },
  { code: "NO", name: "Norway" },
  { code: "SE", name: "Sweden" },
  { code: "DK", name: "Denmark" },
  { code: "FI", name: "Finland" },
  { code: "IE", name: "Ireland" },
  { code: "PL", name: "Poland" },
  { code: "AT", name: "Austria" },
  { code: "CH", name: "Switzerland" },
];

export const CATEGORIES: Category[] = [
  { id: "software-dev", name: "Software Development" },
  { id: "web-dev", name: "Web Development" },
  { id: "cloud", name: "Cloud" },
  { id: "devops", name: "DevOps" },
  { id: "cyber", name: "Cyber Security" },
  { id: "ai", name: "Artificial Intelligence" },
  { id: "ml", name: "Machine Learning" },
  { id: "data-eng", name: "Data Engineering" },
  { id: "erp", name: "ERP" },
  { id: "crm", name: "CRM" },
  { id: "fintech", name: "FinTech" },
  { id: "healthtech", name: "HealthTech" },
  { id: "edtech", name: "EdTech" },
  { id: "recruitment", name: "Recruitment" },
  { id: "consulting", name: "IT Consulting" },
  { id: "digital-xform", name: "Digital Transformation" },
  { id: "automation", name: "Automation" },
  { id: "blockchain", name: "Blockchain" },
  { id: "mobile", name: "Mobile Development" },
  { id: "saas", name: "SaaS" },
  { id: "api", name: "API Development" },
];

const CITY_SEED: Record<string, string[]> = {
  DE: ["Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Stuttgart", "Düsseldorf"],
  NL: ["Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven"],
  BE: ["Brussels", "Antwerp", "Ghent", "Leuven"],
  FR: ["Paris", "Lyon", "Marseille", "Toulouse", "Nantes", "Lille"],
  ES: ["Madrid", "Barcelona", "Valencia", "Seville", "Bilbao"],
  IT: ["Milan", "Rome", "Turin", "Bologna", "Florence"],
  PT: ["Lisbon", "Porto", "Braga", "Coimbra"],
  NO: ["Oslo", "Bergen", "Trondheim"],
  SE: ["Stockholm", "Gothenburg", "Malmö", "Uppsala"],
  DK: ["Copenhagen", "Aarhus", "Odense"],
  FI: ["Helsinki", "Espoo", "Tampere", "Oulu"],
  IE: ["Dublin", "Cork", "Galway", "Limerick"],
  PL: ["Warsaw", "Kraków", "Wrocław", "Gdańsk", "Poznań"],
  AT: ["Vienna", "Graz", "Linz", "Salzburg"],
  CH: ["Zurich", "Geneva", "Basel", "Bern", "Lausanne"],
};

export const CITIES: City[] = Object.entries(CITY_SEED).flatMap(([countryCode, names]) =>
  names.map((name) => ({
    id: `${countryCode}-${name.toLowerCase().replace(/\s+/g, "-")}`,
    name,
    countryCode,
  })),
);

const INDUSTRIES = ["Software", "Cloud", "FinTech", "HealthTech", "Consulting", "SaaS"];

const NAME_PREFIX = [
  "Nimbus",
  "Nordic",
  "Alpine",
  "Baltic",
  "Horizon",
  "Vertex",
  "Quantum",
  "Aurora",
  "Helix",
  "Pulse",
  "Stack",
  "Forge",
  "Lattice",
  "Orbit",
  "Cedar",
];

const NAME_SUFFIX = [
  "Labs",
  "Systems",
  "Soft",
  "Digital",
  "Works",
  "Cloud",
  "Analytics",
  "Security",
  "Apps",
  "Partners",
];

function hash(input: string): number {
  let h = 0;
  for (let i = 0; i < input.length; i += 1) {
    h = (h * 31 + input.charCodeAt(i)) >>> 0;
  }
  return h;
}

/** Deterministic dummy companies per city so UI feels real without a backend. */
export const COMPANIES: Company[] = CITIES.flatMap((city) => {
  const count = 4 + (hash(city.id) % 5);
  return Array.from({ length: count }, (_, index) => {
    const seed = `${city.id}-${index}`;
    const h = hash(seed);
    const name = `${NAME_PREFIX[h % NAME_PREFIX.length]} ${NAME_SUFFIX[(h >> 3) % NAME_SUFFIX.length]}`;
    const categoryCount = 2 + (h % 4);
    const categoryIds = Array.from({ length: categoryCount }, (__, cIdx) => {
      return CATEGORIES[(h + cIdx * 5) % CATEGORIES.length].id;
    });
    const uniqueCategoryIds = [...new Set(categoryIds)];
    const slug = name.toLowerCase().replace(/\s+/g, "");
    return {
      id: `co-${seed}`,
      name,
      website: `https://www.${slug}.example`,
      industry: INDUSTRIES[h % INDUSTRIES.length],
      cityId: city.id,
      countryCode: city.countryCode,
      categoryIds: uniqueCategoryIds,
    };
  });
});
