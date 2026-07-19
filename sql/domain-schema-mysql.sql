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

CREATE TABLE TB_USER (
    ID         BIGINT AUTO_INCREMENT PRIMARY KEY,
    EMAIL      VARCHAR(254) NOT NULL,
    PASSWORD   VARCHAR(100) NOT NULL,
    ACTIVE     BOOLEAN      NOT NULL,
    CREATED_AT DATETIME     NOT NULL,
    CONSTRAINT UK_TB_USER_EMAIL UNIQUE (EMAIL)
) ENGINE=Inno

CREATE TABLE TB_EMAIL_VERIFICATION (
    EMAIL      VARCHAR(254) NOT NULL PRIMARY KEY,
    PASSWORD   VARCHAR(100) NOT NULL,
    CODE       VARCHAR(10)  NOT NULL,
    EXPIRES_AT DATETIME     NOT NULL
) ENGINE=InnoDB;
