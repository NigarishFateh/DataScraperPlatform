import type { AuthTokens, AuthUser } from "../../types/auth";
import { persistTokens, postJson, tryRefresh } from "../api/client";
import {
  clearSession,
  getRefreshToken,
  getStoredUser,
  hasSession,
} from "../storage/tokenStorage";

const AUTH_MODE = (import.meta.env.VITE_AUTH_MODE ?? "google") as "dev" | "google";

/**
 * Authentication use-cases for the Side Panel.
 *
 * Modes:
 * - dev: POST /api/auth/dev-login (learn JWT flow without Google Cloud)
 * - google: chrome.identity.getAuthToken → backend /api/auth/google
 */
export async function loginWithGoogle(): Promise<AuthUser> {
  if (AUTH_MODE === "dev") {
    return loginDev();
  }

  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim();
  if (!clientId) {
    throw new Error(
      "Missing VITE_GOOGLE_CLIENT_ID. Add your Chrome extension OAuth client ID to chrome-extension/.env and rebuild.",
    );
  }

  const accessToken = await requestGoogleAccessToken();
  const tokens = await postJson<AuthTokens>("/api/auth/google", { accessToken });
  await persistTokens(tokens);
  return {
    id: String(tokens.user.id),
    email: tokens.user.email,
    displayName: tokens.user.displayName,
    pictureUrl: tokens.user.pictureUrl,
  };
}

export async function loginDev(email = "analyst@leadintelligence.local"): Promise<AuthUser> {
  const tokens = await postJson<AuthTokens>("/api/auth/dev-login", {
    email,
    displayName: "Lead Analyst",
  });
  await persistTokens(tokens);
  return {
    id: String(tokens.user.id),
    email: tokens.user.email,
    displayName: tokens.user.displayName,
    pictureUrl: tokens.user.pictureUrl,
  };
}

export async function restoreSession(): Promise<AuthUser | null> {
  if (!(await hasSession())) {
    return null;
  }

  const existing = await getStoredUser();
  if (existing) {
    return existing;
  }

  const refreshed = await tryRefresh();
  if (!refreshed) {
    return null;
  }
  return getStoredUser();
}

export async function logout(): Promise<void> {
  const refreshToken = await getRefreshToken();
  try {
    if (refreshToken) {
      await postJson("/api/auth/logout", { refreshToken });
    }
  } catch {
    // Always clear local session even if server revoke fails.
  }
  await clearSession();

  if (AUTH_MODE === "google" && chrome.identity?.clearAllCachedAuthTokens) {
    await new Promise<void>((resolve) => {
      chrome.identity.clearAllCachedAuthTokens(() => resolve());
    });
  }
}

function requestGoogleAccessToken(): Promise<string> {
  return new Promise((resolve, reject) => {
    if (!chrome.identity?.getAuthToken) {
      reject(new Error("chrome.identity is unavailable. Check manifest identity permission."));
      return;
    }
    chrome.identity.getAuthToken({ interactive: true }, (token) => {
      const err = chrome.runtime.lastError;
      if (err || !token) {
        reject(new Error(err?.message ?? "Google sign-in was cancelled"));
        return;
      }
      resolve(token);
    });
  });
}

export function authModeLabel(): string {
  return AUTH_MODE === "google" ? "Google OAuth" : "Dev login";
}
