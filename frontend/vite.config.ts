import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'

export default defineConfig({
  root: fileURLToPath(new URL('.', import.meta.url)),
  plugins: [vue()],
  build: { outDir: 'dist', emptyOutDir: true, target: 'es2022' },
  server: {
    host: '127.0.0.1',
    proxy: {
      '/api': { target: process.env.LAB_DEV_API_URL || 'http://127.0.0.1:8080', changeOrigin: false },
    },
  },
})
