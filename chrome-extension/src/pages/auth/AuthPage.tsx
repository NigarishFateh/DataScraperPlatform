import { useNavigate } from "react-router-dom";
import { BrandMark } from "../../components/layout/BrandMark";
import { useAuth } from "../../hooks/useAuth";
import { authModeLabel } from "../../services/auth/authService";

/**
 * Screen 1 — Authentication.
 * Google OAuth (or dev login) → Auth Service → JWT + refresh → chrome.storage.
 */
export function AuthPage() {
  const navigate = useNavigate();
  const { login, authenticating, error } = useAuth();

  async function onContinue() {
    try {
      await login();
      navigate("/dashboard", { replace: true });
    } catch {
      // Error surfaced via AuthContext.
    }
  }

  return (
    <div className="flex flex-1 flex-col justify-center gap-8 py-6">
      <div className="space-y-5 text-center">
        <div className="mx-auto w-fit animate-[fadeIn_500ms_ease-out]">
          <BrandMark />
        </div>
        <div className="space-y-2 animate-[fadeIn_700ms_ease-out]">
          <h1 className="font-display text-2xl font-semibold tracking-tight text-mist-100">
            Welcome back
          </h1>
          <p className="mx-auto max-w-[18rem] text-sm leading-relaxed text-mist-300">
            Sign in to run global business intelligence scraping jobs from Chrome.
          </p>
        </div>
      </div>

      <div className="li-surface space-y-3 p-4 animate-[fadeIn_900ms_ease-out]">
        <button
          type="button"
          className="li-btn-primary disabled:cursor-not-allowed disabled:opacity-60"
          onClick={() => void onContinue()}
          disabled={authenticating}
        >
          <GoogleGlyph />
          {authenticating
            ? "Signing in…"
            : authModeLabel() === "Dev login"
              ? "Continue (dev login)"
              : "Continue with Google"}
        </button>
        {error ? (
          <p className="text-center text-[11px] leading-relaxed text-red-300">{error}</p>
        ) : (
          <p className="text-center text-[11px] leading-relaxed text-mist-400">
            {authModeLabel() === "Google OAuth"
              ? "Sign in with your Google account. Identity is verified by the Auth Service."
              : "Mode: Dev login. Tokens are issued by the Auth Service, not by this UI."}
          </p>
        )}
      </div>

      <p className="text-center text-[11px] text-mist-400">
        Public data only · robots.txt respected · API-first when available
      </p>

      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(8px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}

function GoogleGlyph() {
  return (
    <svg width="16" height="16" viewBox="0 0 48 48" aria-hidden>
      <path
        fill="#FFC107"
        d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.3 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.1 8 3l5.7-5.7C34.2 6.1 29.4 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.5-.4-3.5z"
      />
      <path
        fill="#FF3D00"
        d="M6.3 14.7l6.6 4.8C14.7 16.1 19 13 24 13c3.1 0 5.8 1.1 8 3l5.7-5.7C34.2 6.1 29.4 4 24 4 16.3 4 9.7 8.3 6.3 14.7z"
      />
      <path
        fill="#4CAF50"
        d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.3 35.4 26.8 36 24 36c-5.3 0-9.7-3.3-11.3-8l-6.5 5C9.5 39.6 16.2 44 24 44z"
      />
      <path
        fill="#1976D2"
        d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.1-4.1 5.5l.1.1 6.2 5.2C39.2 37.1 44 32 44 24c0-1.3-.1-2.5-.4-3.5z"
      />
    </svg>
  );
}
