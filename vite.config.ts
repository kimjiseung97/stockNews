import { defineConfig } from "vite";
import path from "path";
import { fileURLToPath } from "url";
import react, { reactCompilerPreset } from "@vitejs/plugin-react";
import babel from "@rolldown/plugin-babel";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export default defineConfig(({ mode }) => {
  const isProduction = mode === "production";

  return {
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },

    // 전역 scss
    css: {
      modules: {
        generateScopedName: isProduction ? "[hash:base64:7]" : "[name]__[local]__[hash:base64:5]",
      },
      preprocessorOptions: {
        scss: {
          additionalData: `
          @import "@/assets/styles/common/variable.scss"; 
          @import "@/assets/styles/common/mixin/flexMixIn.scss";
          @import "@/assets/styles/common/mixin/btnMixin.scss";
          @import "@/assets/styles/common/mixin/ModalMixIn.scss";
          `,
        },
      },
    },
    build: {
      minify: "terser", // 기본 esbuild보다 강력한 terser 사용
      terserOptions: {
        compress: {
          drop_console: true, // 운영 환경에서 console.log 제거
          drop_debugger: true,
        },
        mangle: true, // 변수명, 함수명 난독화
        format: {
          comments: false, // 모든 주석 제거
        },
      },
      sourcemap: false, // 빌드 시 소스 맵 생성을 방지하여 원본 코드 노출 차단
    },
    plugins: [react(), babel({ presets: [reactCompilerPreset()] })],
  };
});
