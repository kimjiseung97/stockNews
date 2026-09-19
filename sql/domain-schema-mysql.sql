-- Reference DDL for domain tables under model/table/.
-- NOTE: spring.jpa.hibernate.ddl-auto=update already creates/evolves these
-- tables automatically at startup. This script is a manual/reference copy
-- for cases where you need to create the schema without running the app
-- (e.g. provisioning a fresh database). No physical FK constraints, per
-- project convention (logical/JPA-level relationships only).

CREATE TABLE TB_STOCK (
    ID                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    TICKER              VARCHAR(10)  NOT NULL,
    NAME                VARCHAR(100) NOT NULL,
    CIK                 BIGINT,
    THEME               VARCHAR(30),
    KOREAN_NAME         VARCHAR(100),
    DETAIL_ATTEMPTED_AT DATETIME,
    STATUS              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    DELISTED_AT         DATETIME,
    CONSTRAINT UK_TB_STOCK_TICKER UNIQUE (TICKER)
) ENGINE=InnoDB;

CREATE INDEX IDX_TB_STOCK_THEME ON TB_STOCK (THEME);

-- STOCK_ID는 TB_STOCK.ID를 논리적으로 참조 (물리 FK 없음, 프로젝트 컨벤션).
CREATE TABLE TB_STOCK_DETAIL (
    ID                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    STOCK_ID            BIGINT       NOT NULL,
    SUMMARY             VARCHAR(2000),
    REPRESENTATIVE_NAME VARCHAR(100),
    NATION              VARCHAR(50),
    CITY                VARCHAR(100),
    HOMEPAGE_URL        VARCHAR(255),
    INDUSTRY_NAME       VARCHAR(100),
    LISTED_AT           DATETIME,
    CONSTRAINT UK_TB_STOCK_DETAIL_STOCK_ID UNIQUE (STOCK_ID)
) ENGINE=InnoDB;

CREATE TABLE TB_USER (
    ID                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    EMAIL               VARCHAR(254) NOT NULL,
    PASSWORD            VARCHAR(100) NOT NULL,
    RECOVERY_EMAIL      VARCHAR(254),
    ACTIVE              BOOLEAN      NOT NULL,
    TEMPORARY_PASSWORD  BOOLEAN      NOT NULL,
    CREATED_AT          DATETIME     NOT NULL,
    CONSTRAINT UK_TB_USER_EMAIL UNIQUE (EMAIL),
    CONSTRAINT UK_TB_USER_RECOVERY_EMAIL UNIQUE (RECOVERY_EMAIL)
) ENGINE=InnoDB;

-- 유저별 뉴스 다이제스트 메일 발송 여부. 회원가입 완료 시 기본값(true)으로 row가 함께 생성된다.
-- 설정 row가 없는 유저(과거 가입자 등)는 배치에서 기본 발송 대상으로 취급.
-- USER_ID는 TB_USER.ID를 논리적으로 참조 (물리 FK 없음, 프로젝트 컨벤션).
CREATE TABLE TB_USER_MAILSEND_SETTING (
    ID           BIGINT AUTO_INCREMENT PRIMARY KEY,
    USER_ID      BIGINT  NOT NULL,
    MAIL_ENABLED BOOLEAN NOT NULL,
    CONSTRAINT UK_TB_USER_MAILSEND_SETTING_USER UNIQUE (USER_ID)
) ENGINE=InnoDB;

-- 유저별 뉴스 다이제스트 메일 발송 시간대. 회원가입 완료 시 기본값(09:00)으로 row가 함께 생성된다.
-- USER_ID는 TB_USER.ID를 논리적으로 참조 (물리 FK 없음, 프로젝트 컨벤션).
CREATE TABLE TB_USER_MAIL_DISPATCH_SETTING (
    ID            BIGINT AUTO_INCREMENT PRIMARY KEY,
    USER_ID       BIGINT NOT NULL,
    DISPATCH_TIME TIME   NOT NULL,
    CONSTRAINT UK_TB_USER_MAIL_DISPATCH_SETTING_USER UNIQUE (USER_ID)
) ENGINE=InnoDB;

-- PK가 EMAIL이 아닌 auto-increment ID인 이유: 10분 내 재발송 횟수를 세려면(COUNT ... WHERE
-- EMAIL=? AND CREATED_AT > ?) 요청마다 row가 남아야 하므로 email당 단일 row를 덮어쓰지 않는다.
CREATE TABLE TB_EMAIL_VERIFICATION (
    ID            BIGINT AUTO_INCREMENT PRIMARY KEY,
    EMAIL         VARCHAR(254) NOT NULL,
    CODE          VARCHAR(10)  NOT NULL,
    EXPIRES_AT    DATETIME     NOT NULL,
    VERIFIED      BOOLEAN      NOT NULL,
    CREATED_AT    DATETIME     NOT NULL,
    ATTEMPT_COUNT INT          NOT NULL
) ENGINE=InnoDB;

CREATE INDEX IDX_TB_EMAIL_VERIFICATION_EMAIL ON TB_EMAIL_VERIFICATION (EMAIL);

-- PURPOSE stores the VerificationPurpose enum (FIND_EMAIL, RESET_PASSWORD) as its name via
-- @Enumerated(EnumType.STRING); IDENTIFIER holds whichever email the flow keys off
-- (recoveryEmail for FIND_EMAIL, login email for RESET_PASSWORD).
-- (IDENTIFIER, PURPOSE) 유니크 제약 없음 — 10분 내 재요청 횟수를 세려면 요청마다 row가 쌓여야 한다.
CREATE TABLE TB_VERIFICATION (
    ID            BIGINT AUTO_INCREMENT PRIMARY KEY,
    IDENTIFIER    VARCHAR(254) NOT NULL,
    PURPOSE       VARCHAR(30)  NOT NULL,
    CODE          VARCHAR(10)  NOT NULL,
    EXPIRES_AT    DATETIME     NOT NULL,
    VERIFIED      BOOLEAN      NOT NULL,
    CREATED_AT    DATETIME     NOT NULL,
    ATTEMPT_COUNT INT          NOT NULL
) ENGINE=InnoDB;

CREATE INDEX IDX_TB_VERIFICATION_IDENTIFIER_PURPOSE ON TB_VERIFICATION (IDENTIFIER, PURPOSE);

-- 종목 검색 통계용 로그 테이블 - 검색 1회당 1행 적재, 통계는 STOCK_ID로 GROUP BY해서 집계.
-- STOCK_ID는 TB_STOCK.ID를 논리적으로 참조 (물리 FK 없음, 프로젝트 컨벤션).
CREATE TABLE TB_STOCK_SEARCH_COUNT (
    ID         BIGINT AUTO_INCREMENT PRIMARY KEY,
    STOCK_ID   BIGINT   NOT NULL,
    CREATED_AT DATETIME NOT NULL
) ENGINE=InnoDB;

CREATE INDEX IDX_TB_STOCK_SEARCH_COUNT_STOCK_ID ON TB_STOCK_SEARCH_COUNT (STOCK_ID);

-- 종목별 뉴스 수집 배치가 적재하는 테이블. 웹사이트 종목별 뉴스 탭 조회용, 7일 보관 후 정리 배치가 삭제.
-- STOCK_ID는 TB_STOCK.ID를 논리적으로 참조 (물리 FK 없음, 프로젝트 컨벤션).
CREATE TABLE TB_STOCK_NEWS (
    ID           BIGINT AUTO_INCREMENT PRIMARY KEY,
    STOCK_ID     BIGINT       NOT NULL,
    TITLE        VARCHAR(500) NOT NULL,
    CONTENT      VARCHAR(4000),
    URL          VARCHAR(500) NOT NULL,
    COLLECTED_AT DATETIME     NOT NULL,
    -- newsEmbeddingJob이 임베딩 서비스로 넘긴 시각. NULL이면 아직 전송 대기.
    EMBEDDED_AT  DATETIME(6)  NULL,
    CONSTRAINT UK_TB_STOCK_NEWS_STOCK_URL UNIQUE (STOCK_ID, URL)
) ENGINE=InnoDB;

CREATE INDEX IDX_TB_STOCK_NEWS_STOCK_ID ON TB_STOCK_NEWS (STOCK_ID);
CREATE INDEX IDX_TB_STOCK_NEWS_COLLECTED_AT ON TB_STOCK_NEWS (COLLECTED_AT);
-- 조건(EMBEDDED_AT IS NULL) + 정렬(ID ASC)을 한 인덱스로 처리하기 위한 복합 인덱스.
CREATE INDEX IDX_TB_STOCK_NEWS_EMBEDDED_AT_ID ON TB_STOCK_NEWS (EMBEDDED_AT, ID);

-- 기존 DB에 적용할 때(MySQL 대상). Hibernate ddl-auto=update는 MariaDB 문법인
-- `alter table if exists ...`를 내보내서 MySQL 서버가 1064로 거절하므로 컬럼이 자동 추가되지 않는다.
-- 배포 대상 MariaDB에서는 자동 추가되지만, 로컬 Aiven MySQL에는 아래를 직접 실행해야 한다.
-- ALTER TABLE TB_STOCK_NEWS ADD COLUMN EMBEDDED_AT DATETIME(6) NULL;
-- ALTER TABLE TB_STOCK_NEWS ADD INDEX IDX_TB_STOCK_NEWS_EMBEDDED_AT_ID (EMBEDDED_AT, ID);
