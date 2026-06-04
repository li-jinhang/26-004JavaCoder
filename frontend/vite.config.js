import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 26004,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL || 'http://localhost:26904',
        changeOrigin: true
      }
    }
  }
})
