import { apiFetch } from "../api/client";
import type { LeadershipLookupResponse } from "../../types/leadership";

export async function fetchLeadership(
  companyNames: string[],
): Promise<LeadershipLookupResponse> {
  const response = await apiFetch("/api/discovery/leadership/nl-restaurants", {
    method: "POST",
    body: JSON.stringify({ companyNames }),
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch leadership (${response.status})`);
  }
  return (await response.json()) as LeadershipLookupResponse;
}

/** Priority NL QSR brands used as a one-click custom seed. */
export const NL_RESTAURANT_BRANDS = [
  "FEBO",
  "Smullers",
  "Kwalitaria",
  "Bram Ladage",
  "Manneken Pis",
  "Vlaams Friteshuis Vleminckx",
  "La Place",
  "McDonald's",
  "Burger King",
  "KFC",
  "Subway",
  "Five Guys",
  "Taco Bell",
  "Pizza Hut",
  "Domino's Pizza",
  "New York Pizza",
  "Papa John's",
  "Work to Go",
  "Bagels & Beans",
  "Dunkin'",
] as const;
