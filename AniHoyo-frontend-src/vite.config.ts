import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react-swc'
import path from 'path';
import dns from 'dns';

dns.setDefaultResultOrder('verbatim')
// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [
      react(), 
      // visualizer() as PluginOption
    ],
    server: {
      port: Number(env.PORT) || Number(process.env.PORT) || 5173,
      strictPort: true
    },
    optimizeDeps: {
    esbuildOptions: {
      define: {
        global: 'window', // Define 'global' as 'window' for browser compatibility
      },
    },
  },
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src/"),
        components: `${path.resolve(__dirname, "./src/components/")}`,
        styles: `${path.resolve(__dirname, "./src/styles/")}`,
        config: `${path.resolve(__dirname, "./src/config/")}`,
        pages: `${path.resolve(__dirname, "./src/pages/")}`,
        assets: `${path.resolve(__dirname, "./src/assets/")}`,
      },
    },
  }
})
