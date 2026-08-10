-- Reference DDL for domain tables under model/table/.
-- NOTE: spring.jpa.hibernate.ddl-auto=update already creates/evolves these
-- tables automatically at startup. This script is a manual/reference copy
-- for cases where you need to create the schema without running the app
-- (e.g. provisioning a fresh database). No physical FK constraints, per
-- project convention (logical/JPA-level relationships only).

CREATE TABLE TB_STOCK (
    ID     BIGINT AUTO_INCREMENT PRIMARY KEY,
    TICKER VARCHAR(10)  NOT NULL,
    NAME   VARCHAR(100) NOT NULL,
    CIK    BIGINT       NOT NULL,
    THEME  VARCHAR(30),
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
