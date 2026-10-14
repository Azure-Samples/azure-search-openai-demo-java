import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [react()],
    resolve: {
        preserveSymlinks: true
    },
    build: {
        outDir: "./build",
        emptyOutDir: true,
        sourcemap: true,
        rollupOptions: {
            output: {
                manualChunks: id => {
                    if (id.includes("@fluentui/react-icons")) {
                        return "fluentui-icons";
                    } else if (id.includes("@fluentui/react")) {
                        return "fluentui-react";
                    } else if (id.includes("node_modules")) {
                        return "vendor";
                    }
                }
            }
        },
        target: "esnext"
    },

 
    server: {
        proxy: {
            "/api/": {
                target: "http://localhost:8080",
                changeOrigin: true
            },
            
            "/auth_setup": {
                target: "http://localhost:8080/api",
                changeOrigin: true
            },
            "/.auth/me": {
                target: "http://localhost:8080/api",
                changeOrigin: true
            }
        }
    }


    
});
