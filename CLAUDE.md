# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1 / Kotlin 2.3 backend that seeds a US-stock universe from external data sources and (eventually) dispatches a daily news email batch. Package root: `org.kjs.stocknews`. Not currently a git repository.

## Commands

```
./gradlew build            # compile + run tests
./gradlew test             # run all tests
./gradlew test --tests "org.kjs.stocknews.StockNewsApplicationTests"   # single test class
./gradlew bootRun           # run the app locally
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Configuration

- `src/main/resources/application.yml` is git-ignored ("Local secrets") — it holds live DB and FMP API credentials. When editing config, keep secrets out of any file that isn't already ignored, and prefer the `${ENV_VAR:default}` pattern already used there.
- Datasource is a remote Aiven-hosted MySQL instance. `spring.jpa.hibernate.ddl-auto: update` — schema evolves via Hibernate auto-update, not migration scripts, except for the Spring Batch metadata tables below.
- Spring Batch's own metadata tables (`BATCH_JOB_INSTANCE`, `BATCH_STEP_EXECUTION`, etc.) are **not** auto-created by Spring Boot 4.1's batch autoconfiguration. They must be created once manually by running `sql/batch-schema-mysql.sql` against the target database.
- `StockNewsApplication` explicitly excludes `BatchJobLauncherAutoConfiguration` — batch jobs are not run automatically on startup; they're triggered on demand (see Architecture below).

## Architecture

**Sync MVC + on-demand async batch jobs.** The web layer (`controller/`) is a conventional synchronous Spring MVC REST API. Spring Batch jobs are launched explicitly through `JobOperator` from a controller endpoint rather than via a scheduler or the batch autoconfiguration's auto-run-on-startup behavior — e.g. `POST /admin/stocks/seed` in `StockAdminController` starts `stockSeedJob` and immediately returns the job execution id (fire-and-forget, does not block on completion). The intended scope for real async/parallel work is the future daily news-email dispatch batch job, not the API itself.

**Batch job configs** (`batch/*JobConfig.kt`) each define a `Job` + `Step` pair as `@Bean`s using the builder DSL. Note the Spring Batch 6 package layout used throughout — these differ from most online tutorials/examples written against Batch 5 and earlier:
  - `org.springframework.batch.core.job.Job` / `.job.builder.JobBuilder` (not `org.springframework.batch.core.Job`/`JobBuilder`)
  - `org.springframework.batch.core.step.Step` / `.step.builder.StepBuilder`
  - `org.springframework.batch.infrastructure.repeat.RepeatStatus` (not `org.springframework.batch.core.repeat.RepeatStatus`)

Stock seeding is split into two independent jobs so the slow, sequential per-company lookup doesn't gate how fast the ticker universe grows:
- `StockSeedJobConfig`: single tasklet that fetches the full SEC ticker list (`SecTickerClient`), filters out tickers already in `TB_STOCK`, takes `stock.seed.batch-size` new candidates, and bulk-inserts them as `Stock(ticker, name, cik)` with `theme = null`. No external calls beyond the one bulk ticker-list fetch.
- `StockThemeEnrichJobConfig`: single tasklet that pulls up to `stock.theme-enrich.batch-size` rows from `TB_STOCK` where `theme IS NULL`, calls SEC's per-company submissions endpoint (`SecCompanyProfileClient`, keyed by CIK) for each to get a SIC code, maps it to a `StockTheme` via `themeForSic()`, and saves. Rows whose SIC code doesn't map to a known theme are left with `theme = null` and get retried on the next run.
- `NewsDispatchJobConfig`: currently a stub job/step (tasklet just returns `RepeatStatus.FINISHED`) — placeholder for the daily news email batch.

Both stock jobs are also triggered on a cron schedule (`StockSeedScheduler` / `StockThemeEnrichScheduler`, configured via `stock.seed.cron` / `stock.theme-enrich.cron`), in addition to being launchable on demand via the `/admin/stocks/*` endpoints — they are not purely on-demand.

**External clients** (`service/`) are thin `RestClient` wrappers built on the JDK `HttpClient`, each with its own connect/read timeouts:
  - `SecTickerClient` fetches `sec.gov/files/company_tickers.json` (requires a descriptive `User-Agent`, configured via `sec.user-agent`) and parses it into `List<SecTickerEntry>` (ticker, company title, CIK).
  - `SecCompanyProfileClient` calls `data.sec.gov/submissions/CIK{10-digit-cik}.json` for a single company and returns its SIC code/description as `SecCompanyProfile`.
  - The `TB_STOCK` table is seeded exclusively from SEC data (no FMP or other third-party source) and is scoped to the US market only — don't add alternate seeding sources without confirming that's intended.

**Persistence**: JPA entities live in `model/table/`, one repository interface per aggregate in `repository/`. QueryDSL is set up via `kotlin("kapt")` + `querydsl-jpa`/`querydsl-apt` (`:jakarta` classifier) in `build.gradle.kts`, generating `Q*` classes under `build/generated/source/kapt/main` (wired into `sourceSets.main.java`). A single `JPAQueryFactory` bean lives in `config/QuerydslConfig.kt`. For queries beyond what a Spring Data derived/`@Query` method covers, add a `<Aggregate>RepositoryCustom` interface + `<Aggregate>RepositoryCustomImpl` (QueryDSL-backed, `@Repository`) and have the main repository interface extend the custom one — see `StockRepositoryCustom(Impl)` for `findByThemeIsNull`. Entities use identity-generated `Long` PKs with a protected setter (no all-args public id setter). Domain tables use **logical/JPA-level relationships only — no physical foreign-key constraints** in the schema; this was a deliberate choice after past pain with FK-related test maintenance, so don't introduce `@JoinColumn`-backed physical FKs or DB-level `FOREIGN KEY` constraints without discussing it first.

- `User` / `EmailVerification` model an email-based signup/verification flow (verification row is keyed directly by email, holds a pending password + code + expiry) but there's no controller/service wired up for it yet.
- `Stock` holds ticker/name/cik/theme; `theme` is nullable until the enrichment job resolves it. `StockTheme` is a fixed enum of GICS-like sectors mapped from SEC SIC codes via `themeForSic()` in `StockThemeEnrichJobConfig`.

## Conventions

- DB identifiers are upper-snake-case (`@Table(name = "TB_STOCK")`, `@Column(name = "TICKER")`) even though Kotlin properties are camelCase.
- Tables use a `TB_` prefix (`TB_STOCK`, `TB_USER`, `TB_EMAIL_VERIFICATION`) — apply this to any new table going forward.
- DTOs deserializing external API responses use `@JsonIgnoreProperties(ignoreUnknown = true)` and only declare the fields actually consumed.
