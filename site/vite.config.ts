import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  base: "./",
  resolve: {
    alias: {
      "@": "/src",
    },
  },
  server: {
    proxy: {
      "/api/live": {
        target: "http://127.0.0.1:8127",
        changeOrigin: true,
      },
    },
  },
  plugins: [react(), tailwindcss()],
});
