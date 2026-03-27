import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // All API calls routed through the Gateway — single entry point
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})