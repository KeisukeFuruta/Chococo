import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      // バックエンド（Spring Boot, :8080）未着手のためプロトタイプでは未使用。
      // tech-stack.md の構成に合わせて先に定義しておく。
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
})
