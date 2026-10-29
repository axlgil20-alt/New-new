import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Configuración de Vite compatible con exportación a APK usando Capacitor
export default defineConfig({
  plugins: [react()],
  base: './', // Previene pantallas en blanco al cargar assets relativos en WebView de Android
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false
  },
  server: {
    port: 3000,
    host: true
  }
});
