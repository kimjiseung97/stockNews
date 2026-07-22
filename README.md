# Project

## 프론트엔드

---

## 기술 스택

- React
- TypeScript
- SCSS
- Vite
- Axios
- React Router
- Lucide React

---

## 개발 환경

- Node.js
- npm
- Vite
- Visual Studio Code

---

## 설치 라이브러리

### Sass

```bash
npm install sass
```

SCSS 문법을 사용하기 위한 라이브러리

- 공통 변수 관리
- 공통 믹스인 관리
- 페이지별 스타일 관리
- 중첩 문법 사용

#### 현재 프로젝트의 Sass 설치

현재 프로젝트는 Vite에서 권장하는 `sass-embedded`를 사용

```bash
npm install -D sass-embedded
```

`sass`와 `sass-embedded`는 같은 SCSS 파일을 처리하므로 둘 다 설치하지 않음

실제 스타일을 만드는 전역 SCSS는 `main.tsx`에서 한 번만 불러옵니다.

```tsx
import "@/assets/styles/common/reset.scss";
import "@/assets/styles/common/common.scss";
```

변수와 믹스인처럼 SCSS 작성을 도와주는 파일은 Vite의 `additionalData`에 등록

```ts
additionalData: `
  @use "@/assets/styles/common/variable.scss" as *;
`,
```

나중에 `mixin.scss`를 만들면 같은 방법으로 `additionalData`에 등록

### Axios

```bash
npm install axios
```

서버 API에 데이터를 요청하거나 전달할 때 사용

```ts
axios.get("/api/news");
```

### Lucide React

```bash
npm install lucide-react
```

React 컴포넌트 방식으로 아이콘을 사용하기 위한 라이브러리

```tsx
import { Search } from "lucide-react";

function SearchButton() {
  return (
    <button type="button">
      <Search />
      검색
    </button>
  );
}
```

### React Router

```bash
npm install react-router-dom
```

페이지 이동과 URL 경로를 관리하기 위해 사용

- 페이지 라우팅
- 메뉴 이동
- 상세 페이지 이동
- URL 파라미터 관리
- 잘못된 주소 페이지 처리

### Prettier

```bash
npm install -D prettier
```

코드의 들여쓰기와 줄바꿈 형식을 자동으로 정리하기 위해 사용

### Terser

```bash
npm install -D terser
```

운영용 빌드 파일을 압축하기 위해 사용

- JavaScript 파일 압축
- `console` 제거
- `debugger` 제거
- 주석 제거
- 변수명과 함수명 축약

### React Compiler

```bash
npm install -D @rolldown/plugin-babel @babel/core babel-plugin-react-compiler
```

React 컴포넌트의 렌더링을 최적화하기 위해 사용

```ts
plugins: [react(), babel({ presets: [reactCompilerPreset()] })];
```

---

## TypeScript 사용 규칙

### 타입 명시

함수의 매개변수와 반환값에는 타입을 작성

```ts
function getUserName(name: string): string {
  return name;
}
```

### Props 타입 작성

컴포넌트에서 사용하는 Props는 타입을 먼저 정의

```tsx
type ButtonProps = {
  text: string;
  disabled?: boolean;
};

function Button({ text, disabled = false }: ButtonProps) {
  return (
    <button type="button" disabled={disabled}>
      {text}
    </button>
  );
}
```

### 객체 타입 작성

객체를 사용하기 전에 데이터 구조를 타입으로 정의

```ts
type Stock = {
  id: number;
  symbol: string;
  name: string;
};
```

### 배열 타입 작성

```ts
const stockList: Stock[] = [];
```

### API 응답 타입 작성

API 응답 데이터도 타입을 지정한 후 사용

```ts
type NewsResponse = {
  id: number;
  title: string;
  content: string;
  publishedAt: string;
};
```

## SCSS 관리 규칙

공통으로 사용하는 스타일만 공통 SCSS로 분리

- 색상 변수
- 폰트 변수
- 공통 버튼
- 공통 믹스인
- 공통 레이아웃

페이지에서만 사용하는 스타일은 해당 페이지 SCSS에서 관리

## 경로 별칭

`@`는 `src` 폴더를 의미

```ts
import Button from "@/components/common/Button";
```

Vite 설정:

```ts
resolve: {
  alias: {
    "@": path.resolve(__dirname, "./src"),
  },
},
```

---

## 프로젝트 실행

### 개발 서버 실행

```bash
npm run dev
```

### 운영용 빌드

```bash
npm run build
```

빌드 결과물은 `dist` 폴더에 생성됩니다.

| 명령어          | 설명                           |
| --------------- | ------------------------------ |
| `npm run dev`   | 개발 서버 실행                 |
| `npm run build` | TypeScript 검사 후 운영용 빌드 |

---

## Git 제외 파일

다음 파일과 폴더는 Git에 올리지 않음

```gitignore
node_modules/
dist/
.env
```

---

## 작업 전 확인사항

1. 작업 전 현재 브랜치를 확인
2. 최신 코드를 내려받은 후 작업
3. 프론트엔드 관련 파일만 수정
4. 개발 완료 후 빌드 오류를 확인

```bash
npm run build
```

---
