import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig({
  // Loaded from file:///android_asset/homepage/ inside Mango-TV's WebView -
  // an absolute base ('/...') would resolve against the filesystem root
  // instead of this directory, so every asset reference has to stay relative.
  base: './',
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  build: {
    target: 'es2020',
    // Fire TV browsers vary widely in capability; keep chunks small and
    // avoid relying on aggressive modern-only output.
    chunkSizeWarningLimit: 600,
  },
})
