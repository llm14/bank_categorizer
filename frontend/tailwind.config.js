/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      // Scoped to LandingPage's "greenbar statement" redesign only - namespaced
      // under `ledger` (colors) and `ledger`/`ledger-body` (fonts) rather than
      // overriding Tailwind's default `sans`/`mono` keys, so the rest of the app
      // (which relies on Tailwind's default font stack via preflight) is unaffected.
      colors: {
        ledger: {
          paper: "#F6F5EF",
          band: "#DCEEDD",
          white: "#FFFFFF",
          ink: "#1F5C4A",
          text: "#1C1E1B",
          muted: "#455249",
          stamp: "#C1442D",
        },
      },
      fontFamily: {
        ledger: ['"IBM Plex Mono"', "ui-monospace", "SFMono-Regular", "monospace"],
        "ledger-body": ['"IBM Plex Sans"', "ui-sans-serif", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
