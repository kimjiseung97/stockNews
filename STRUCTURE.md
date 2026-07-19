# stockNews 소스 구조

Spring Boot 4.1 / Kotlin 2.3 백엔드. 미국 주식 유니버스를 외부 데이터소스로 시딩하고, 향후 일일 뉴스 이메일 배치를 디스패치할 예정. 패키지 루트: `org.kjs.stocknews`.

## 디렉토리 트리

```
src/main/kotlin/org/kjs/stocknews/
├── StockNewsApplication.kt          # 엔트리포인트. BatchJobLauncherAutoConfiguration 제외 (배치 자동실행 방지)
├── batch/
│   ├── StockSeedJobConfig.kt        # stockSeedJob: SEC 티커 -> FMP 프로필 조회 -> STOCKS 시딩
│   └── NewsDispatchJobConfig.kt     # newsDispatchJob: 스텁 (tasklet이 바로 FINISHED 반환)
├── controller/
│   └── StockAdminController.kt      # POST /admin/stocks/seed -> stockSeedJob 비동기 트리거
├── model/
│   ├── dto/
│   │   ├── FmpCompanyProfile.kt     # FMP /profile 응답 (symbol, companyName, sector)
│   │   └── SecTickerEntry.kt        # SEC company_tickers.json 엔트리 (cikStr, ticker, title)
│   └── table/
│       ├── Stock.kt                 # STOCKS 테이블. ticker/name/theme, id는 IDENTITY + protected set
│       ├── StockTheme.kt            # GICS 유사 섹터 enum (IT, FINANCE, HEALTHCARE 등 11종)
│       ├── User.kt                  # USERS 테이블. 이메일 기반 계정 (컨트롤러/서비스 미배선)
│       └── EmailVerification.kt     # EMAIL_VERIFICATIONS 테이블. email이 PK, 코드+만료 보유 (미배선)
├── repository/
│   └── StockRepository.kt           # JpaRepository<Stock, Long> + existsByTicker
└── service/
    ├── SecTickerClient.kt           # sec.gov 티커 목록 fetch (User-Agent 필수, RestClient/JDK HttpClient)
    └── FmpStockClient.kt            # FMP /profile 단건 조회 (symbol -> FmpCompanyProfile)

src/test/kotlin/org/kjs/stocknews/
└── StockNewsApplicationTests.kt     # contextLoads 뿐, 실질 테스트 없음

sql/
└── batch-schema-mysql.sql           # Spring Batch 6.0.4 메타데이터 테이블 DDL (수동 1회 실행 필요)
```

## 요청 흐름 (주식 시딩)

1. `POST /admin/stocks/seed` → `StockAdminController.seed()`
2. `JobOperator.start(stockSeedJob, ...)` — 즉시 `jobExecutionId` 반환 (fire-and-forget, 완료 대기 안 함)
3. `StockSeedJobConfig.stockSeedStep` 태스클릿:
   - `SecTickerClient.fetchAllTickers()` 로 SEC 전체 티커 목록 조회
   - 이미 `STOCKS`에 있는 티커 제외, `fmp.seed-batch-size` 만큼 취함
   - 후보마다 `FmpStockClient.fetchProfile(ticker)` 호출 → sector 획득
   - `THEME_BY_FMP_SECTOR` 매핑으로 FMP sector → `StockTheme` 변환
   - 매핑 실패 시 스킵(저장 안 함, 배치 처리 카운트에는 포함); 성공 시 `Stock` 저장

## 도메인 모델 상태

- `Stock`: 실사용 중, `StockSeedJobConfig`가 유일한 쓰기 경로.
- `User` / `EmailVerification`: 이메일 회원가입/인증 플로우를 위한 엔티티만 존재, 컨트롤러/서비스 레이어 미구현.
- `NewsDispatchJobConfig`: 잡/스텝 골격만 존재하는 스텁 — 실제 이메일 발송 로직 없음.
- 물리 FK 없음 — 모든 연관관계는 JPA/로지컬 레벨.

## 설정

- `application.yml`은 git-ignore 대상 (Aiven MySQL 자격증명 + FMP API 키 보유).
- `spring.jpa.hibernate.ddl-auto: update` — Batch 메타데이터 테이블 제외 스키마는 Hibernate가 자동 관리.
- Batch 메타데이터 테이블(`BATCH_JOB_INSTANCE` 등)은 `sql/batch-schema-mysql.sql`을 수동 1회 실행해야 생성됨.

## 빌드

- Gradle Kotlin DSL, Java 17 toolchain.
- 주요 의존성: `spring-boot-starter-batch`, `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`, `mysql-connector-j` (runtime).
- 외부 API 클라이언트는 JDK `HttpClient` 기반 `RestClient` 래퍼 (`service/` 패키지), 각각 자체 connect/read 타임아웃 보유.

## 미구현 / TODO로 보이는 영역

- `NewsDispatchJobConfig` 실제 로직 (일일 뉴스 이메일 배치)
- `User` / `EmailVerification` 관련 컨트롤러·서비스 (가입/인증 플로우)
- 테스트 커버리지 (`StockNewsApplicationTests`는 컨텍스트 로드만 확인)
