import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import fs from "node:fs";
import { resolve } from "node:path";

/**
 * Side panel is a Vite app. Service worker lives in /public/background.js
 * (copied as-is) so MV3 does not break on hashed shared chunks.
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const googleClientId = env.VITE_GOOGLE_CLIENT_ID?.trim();

  return {
    plugins: [
      react(),
      {
        name: "inject-google-oauth-client-id",
        closeBundle() {
          const manifestPath = resolve(__dirname, "dist/manifest.json");
          if (!fs.existsSync(manifestPath)) {
            return;
          }
          const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8")) as {
            oauth2?: { client_id?: string; scopes?: string[] };
          };
          if (!manifest.oauth2) {
            manifest.oauth2 = { scopes: ["openid", "email", "profile"] };
          }
          if (googleClientId) {
            manifest.oauth2.client_id = googleClientId;
          }
          fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
        },
      },
    ],
    base: "./",
    build: {
      outDir: "dist",
      emptyOutDir: true,
      rollupOptions: {
        input: {
          sidepanel: resolve(__dirname, "sidepanel.html"),
        },
      },
    },
  };
});
