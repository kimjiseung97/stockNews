# CLAUDE.md

이 파일은 Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

Spring Boot(Kotlin) + React 개인 프로젝트. 미국 주식 유니버스를 SEC 데이터로 시딩하고, 사용자가 회원가입 후 관심종목을 등록하면 매일 뉴스 다이제스트 이메일을 발송하는 서비스.

## 스택 & 명령어

- 백엔드: Kotlin 2.3, Spring Boot 4.1, MySQL(Aiven 원격 호스팅), Spring Batch 6, QueryDSL
- 프론트: React 19, Vite, TypeScript, React Router, SCSS Modules
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 단일 테스트: `./gradlew test --tests "org.kjs.stocknews.StockNewsApplicationTests"`
- 로컬 실행(백엔드): `./gradlew bootRun` (Windows는 `gradlew.bat` 사용)
- 프론트 실행: `npm run dev` (저장소 루트에서 — 별도 `frontend/` 디렉토리 아님, 아래 참고)
- 프론트 빌드: `npm run build` (`tsc -b && vite build`)
- 린트: `npm run lint` (oxlint)

## 디렉토리 구조

`backend/` + `frontend/` 분리 구조가 **아니고**, 저장소 루트의 `src/` 하나에 Kotlin 백엔드와 React 프론트가 함께 있는 flat 모노레포입니다.

- `src/main/kotlin/org/kjs/stocknews/` - Spring Boot API
  - `controller/` - REST 컨트롤러 (세션 기반 인증)
  - `service/` - 비즈니스 로직 + 외부 API 클라이언트(SEC, Naver, Finnhub)
  - `repository/` - Spring Data JPA + QueryDSL 커스텀 리포지토리
  - `model/table/` - JPA 엔티티
  - `model/dto/` - 요청/응답 DTO, 외부 API 응답 DTO
  - `batch/` - Spring Batch Job/Step 설정 + 스케줄러
  - `common/` - `ApiResponse`, `GlobalExceptionHandler`, `CustomException`, 세션 유틸
  - `config/` - CORS, QueryDSL, PasswordEncoder 설정
- `src/main.tsx`, `src/App.tsx`, `src/pages/`, `src/components/`, `src/layouts/`, `src/assets/styles/` - React 프론트 (Vite)
- `src/test/kotlin/org/kjs/stocknews/` - 백엔드 테스트
- `sql/` - 배치 메타데이터/도메인 스키마 DDL
- `.github/workflows/deploy.yml`, `Dockerfile`, `docker-compose.yml`, `Caddyfile` - 배포 구성

## Git 전략

- `dev`가 기본 개발 브랜치. 기능 작업은 dev 기반 브랜치에서 진행 후 dev로 병합.
- `dev` → `main`으로 push되면 GitHub Actions가 배포 트리거 (`.github/workflows/deploy.yml`: SSH로 접속해 `git pull` + `docker compose up -d --build`).

## API 기능 개발 완료 시

- 모든 엔드포인트: 입력검증 필수.
- 기능 개발 완료 시 test code 작성 (TDD).
- 기능 개발 후 간략하게 주석 작성.
- 응답 DTO는 Entity 직접 노출 금지, 항상 별도 Response 클래스 사용.
- 에러 응답은 공통 ExceptionHandler로 통일 (개별 try-catch로 임의 응답 금지).

## 배치(schedule) 기능 개발 완료시
- 기능개발후 제대로 작동하는지 짧은 단위시간으로 기동시켜 테스트 해볼 것.
- 데이터 수집 적재 후처리까지 제대로되는지 로그로 확인.
- 기능 개발후 어떤 배치인지 주석작성.

## sql 쿼리 기능 개발시
- where 조건이 2개이상 들어가는 쿼리를 작성하게되면 querydsl로 구현할 것.
- 복잡한 join , in절 등의 쿼리가 필요하게 되면 querydsl로 구현할 것.
- 통계성 , 복잡한 쿼리같이 jpa로 표현하기 힘들다면 mybatis나 native query로 구현할 것

## 완료 기준 (Definition of Done)
- 빌드 성공 + 테스트 통과
- 새 엔드포인트는 README나 API 문서에 한 줄 추가

## 설정(Configuration)

- `src/main/resources/application.yml`은 git-ignore 대상 — DB, 메일, 외부 API 자격증명 보유. 저장소 루트의 `.env.example`이 Docker 배포용 필수 env 변수(`DB_*`, `MAIL_*`, `CORS_ALLOWED_ORIGINS`, `DOMAIN`) 문서 역할. yml에서는 `${ENV_VAR:default}` 패턴 사용, 실제 비밀값은 절대 커밋 금지.
- 데이터소스는 Aiven 원격 MySQL. `spring.jpa.hibernate.ddl-auto: update` — Spring Batch 메타데이터 테이블 제외하고 Hibernate 자동 스키마 관리.
- Spring Batch 메타데이터 테이블(`BATCH_JOB_INSTANCE` 등)은 Spring Boot 4.1 배치 자동설정이 생성하지 않음 — `sql/batch-schema-mysql.sql`을 대상 DB에 1회 수동 실행 필요. `sql/domain-schema-mysql.sql`은 도메인 테이블 DDL 참고용.
- `StockNewsApplication`이 `BatchJobLauncherAutoConfiguration`을 명시적으로 제외 — 배치 잡은 시작 시 자동 실행되지 않음.
- CORS는 설정 기반: `WebConfig`/`CorsProperties`가 `cors.allowed-origins`를 바인딩하고 credential(세션 쿠키) 허용 — 프론트 오리진 추가 시 코드가 아니라 env 값을 수정할 것.
- 배포: `Dockerfile`이 Spring Boot 앱을 빌드하고, `docker-compose.yml`이 TLS 종료용 `Caddy` 리버스 프록시(`Caddyfile`)와 함께 실행.

## 컨벤션

- DB 식별자는 upper-snake-case (`@Table(name = "TB_STOCK")`, `@Column(name = "TICKER")`), Kotlin 프로퍼티는 camelCase 유지.
- 테이블은 `TB_` 프리픽스 사용 (`TB_STOCK`, `TB_USER`, `TB_USER_STOCK`, `TB_EMAIL_VERIFICATION`) — 신규 테이블도 동일하게 적용.
- 외부 API 응답을 역직렬화하는 DTO는 `@JsonIgnoreProperties(ignoreUnknown = true)` 사용, 실제 사용하는 필드만 선언.
- 컨트롤러는 직접 `ApiResponse`를 만들지 않고 원본 값/DTO만 반환 — 래핑은 `ApiResponseAdvice`가 담당. 예상 가능한 실패는 `CustomException(ResultCode.X)`로 던질 것.
- 프론트 페이지는 `src/pages/<케밥-케이스-이름>/<파스칼케이스Page>.tsx` 단위로 기능별 하나씩 위치하고, `src/App.tsx`에서 공통 `MainLayout` 안에 라우팅. 스타일은 CSS 모듈 SCSS(`*.module.scss`)로 컴포넌트 경로를 그대로 미러링해 `src/assets/styles/...` 아래 위치.
