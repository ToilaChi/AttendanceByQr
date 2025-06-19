import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import fs from 'fs';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        // target: 'http://192.168.1.4:8080',
        // target: 'http://172.20.10.6:8080',
        target: process.env.VITE_API_BASE_URL,
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    },
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    allowedHosts: [
      'localhost',
      '172.20.10.6',
      '.ngrok-free.app',
      '.ngrok.io'
    ]
  },
  define: {
    global: 'window'
  }
});
