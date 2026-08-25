import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Builds straight into the Spring Boot app's static resources, so the
// public site is served by the same app/port as everything else - no
// separate frontend server in production. emptyOutDir is false on purpose:
// the admin panel's own static/css, static/js, static/images live in the
// same folder and must not be wiped by a frontend rebuild.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: false,
  },
  server: {
    // So `npm run dev` (port 5173) can call the real backend without CORS
    // setup - same relative /api/... paths work in dev and in production.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
