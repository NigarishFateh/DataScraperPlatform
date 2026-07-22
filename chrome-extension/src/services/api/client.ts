import type { AuthTokens } from "../../types/auth";
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  saveSession,
} from "../storage/tokenStorage";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function parseError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    return body.message ?? response.statusText;
  } catch {
    return response.statusText || "Request failed";
  }
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }

  const accessToken = await getAccessToken();
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response = await fetch(`${API_BASE}${path}`, { ...init, headers });

  if (response.status === 401 && !path.includes("/api/auth/")) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      headers.set("Authorization", `Bearer ${refreshed}`);
      response = await fetch(`${API_BASE}${path}`, { ...init, headers });
    }
  }

  return response;
}

export async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new ApiError(response.status, await parseError(response));
  }
  return (await response.json()) as T;
}

export async function tryRefresh(): Promise<string | null> {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  try {
    const tokens = await postJson<AuthTokens>("/api/auth/refresh", { refreshToken });
    await persistTokens(tokens);
    return tokens.accessToken;
  } catch {
    await clearSession();
    return null;
  }
}

export async function persistTokens(tokens: AuthTokens): Promise<void> {
  await saveSession({
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    expiresAt: Date.now() + tokens.expiresInSeconds * 1000,
    user: {
      id: String(tokens.user.id),
      email: tokens.user.email,
      displayName: tokens.user.displayName,
      pictureUrl: tokens.user.pictureUrl,
    },
  });
}
