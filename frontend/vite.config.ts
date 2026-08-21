import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      // aws-infra-design.md 3.4節：写真配信用。本番はNginxが同一オリジンで配信するため、
      // 開発環境でも同様にViteプロキシで:8080へ転送する
      "/uploads": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
})
