import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  build: {
    rollupOptions: {
      output: {
        // Vendor code changes far less often than app code, so give it its own cacheable files.
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          clerk: ['@clerk/clerk-react'],
          axios: ['axios'],
        },
      },
    },
  },
})
