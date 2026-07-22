import type { AuthUser, StoredSession } from "../../types/auth";

const ACCESS_KEY = "li.accessToken";
const REFRESH_KEY = "li.refreshToken";
const EXPIRES_KEY = "li.expiresAt";
const USER_KEY = "li.user";

/**
 * Token storage facade over chrome.storage.
 * Access token → session (cleared when browser closes).
 * Refresh token → local (survives restart; revoked server-side on logout).
 */
export async function saveSession(session: StoredSession): Promise<void> {
  await chrome.storage.session.set({
    [ACCESS_KEY]: session.accessToken,
    [EXPIRES_KEY]: session.expiresAt,
    [USER_KEY]: session.user,
  });
  await chrome.storage.local.set({
    [REFRESH_KEY]: session.refreshToken,
  });
}

export async function clearSession(): Promise<void> {
  await chrome.storage.session.remove([ACCESS_KEY, EXPIRES_KEY, USER_KEY]);
  await chrome.storage.local.remove([REFRESH_KEY]);
}

export async function getAccessToken(): Promise<string | null> {
  const data = await chrome.storage.session.get([ACCESS_KEY]);
  const token = data[ACCESS_KEY];
  return typeof token === "string" ? token : null;
}

export async function getRefreshToken(): Promise<string | null> {
  const data = await chrome.storage.local.get([REFRESH_KEY]);
  const token = data[REFRESH_KEY];
  return typeof token === "string" ? token : null;
}

export async function getStoredUser(): Promise<AuthUser | null> {
  const data = await chrome.storage.session.get([USER_KEY]);
  const user = data[USER_KEY];
  return user && typeof user === "object" ? (user as AuthUser) : null;
}

export async function getExpiresAt(): Promise<number | null> {
  const data = await chrome.storage.session.get([EXPIRES_KEY]);
  const value = data[EXPIRES_KEY];
  return typeof value === "number" ? value : null;
}

export async function hasSession(): Promise<boolean> {
  const refresh = await getRefreshToken();
  const access = await getAccessToken();
  return Boolean(refresh || access);
}
