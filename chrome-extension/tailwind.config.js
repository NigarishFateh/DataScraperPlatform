/** @type {import('tailwindcss').Config} */
export default {
  content: ["./sidepanel.html", "./src/**/*.{js,ts,jsx,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        ink: {
          950: "#070b12",
          900: "#0c1220",
          800: "#121a2b",
          700: "#1a2438",
          600: "#243049",
        },
        mist: {
          100: "#e8eef8",
          200: "#c5d0e3",
          300: "#8fa0bd",
          400: "#6b7c99",
        },
        signal: {
          DEFAULT: "#3dcea7",
          dim: "#2a9f80",
          glow: "rgba(61, 206, 167, 0.18)",
        },
      },
      fontFamily: {
        display: ['"Sora"', "system-ui", "sans-serif"],
        body: ['"DM Sans"', "system-ui", "sans-serif"],
      },
      boxShadow: {
        panel: "0 0 0 1px rgba(255,255,255,0.04), 0 18px 40px rgba(0,0,0,0.35)",
      },
      backgroundImage: {
        atmosphere:
          "radial-gradient(1200px 600px at 10% -10%, rgba(61,206,167,0.12), transparent 55%), radial-gradient(900px 500px at 110% 10%, rgba(88,140,255,0.10), transparent 50%), linear-gradient(180deg, #0c1220 0%, #070b12 100%)",
      },
    },
  },
  plugins: [],
};
