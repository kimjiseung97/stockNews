import { defineConfig } from 'vite'
import path from 'path'
import { fileURLToPath } from 'url'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export default defineConfig(({ mode }) => {
  const isProduction = mode === 'production'

  return {
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },

    // 로컬 개발 시 프론트(Vite, 3000)와 백엔드(Spring Boot, 8080)가 분리 실행되므로
    // 백엔드 API 요청을 전달. 운영에서는 Caddy가 같은 오리진으로 서빙해 프록시가 필요 없음.
    server: {
      proxy: {
        '/auth': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/stocks': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/users': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },

    // 전역 scss
    css: {
      modules: {
        generateScopedName: isProduction ? '[hash:base64:7]' : '[name]__[local]__[hash:base64:5]',
      },
      preprocessorOptions: {
        scss: {
          additionalData: `
  
          @use "@/assets/styles/common/variable.scss" as *;
          @use "@/assets/styles/mixIn/flexMixIn.scss" as *;
          @use "@/assets/styles/mixIn/gridMixIn.scss" as *;
          @use "@/assets/styles/mixIn/btnMixIn.scss" as *;
          @use "@/assets/styles/mixIn/commonStyleMixIn.scss" as *;
        
          `,
        },
      },
    },
    build: {
      minify: 'terser', // 기본 esbuild보다 강력한 terser 사용
      terserOptions: {
        compress: {
          // drop_console: true, // 운영 환경에서 console.log 제거
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
  }
})
