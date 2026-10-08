import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development, requests to /api (REST) and /ws (WebSocket) are forwarded to the Spring Boot
// backend, so the browser sees a single origin (http://localhost:5173).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
});
