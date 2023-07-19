import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [react()],
    build: {
        outDir: "../backend/src/main/resources/static",
        emptyOutDir: true,
        sourcemap: true
    },
    server: {
        proxy: {
            "/api/ask": {
                    target: 'http://localhost:8080',
                    changeOrigin: true
                  },
            "/api/chat": {
                     target: 'http://localhost:8080',
                     changeOrigin: true
                                },
            "/api/content": {
                     target: 'http://localhost:8080',
                     changeOrigin: true
                                            }
        }
    }
});
