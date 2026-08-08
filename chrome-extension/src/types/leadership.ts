export type LeadershipPerson = {
  companyName: string;
  website: string | null;
  leaderName: string | null;
  leadershipTitle: string | null;
  source: string;
  found: boolean;
  ticker?: string | null;
  compensation?: number | null;
};

export type LeadershipLookupResponse = {
  requested: number;
  found: number;
  notes: string;
  results: LeadershipPerson[];
};
