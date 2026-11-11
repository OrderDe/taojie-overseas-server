-- Migration for databases created before brand management landed.
-- Run once against an existing overseas database:
--   mysql -u root -p taojie_overseas < sql/admin-brands.sql
-- Safe to re-run: the CREATE uses IF NOT EXISTS. The ALTER will report
-- "Duplicate column name 'brand_id'" if it was already applied — that
-- error is expected on a second run and means there is nothing to do.

CREATE TABLE IF NOT EXISTS eb_overseas_brand (
    id          VARCHAR(64)  NOT NULL COMMENT '品牌 id（如 kapci）',
    names       JSON         NOT NULL COMMENT '多语言名称 {"en":..,"zh":..,"es":..,"fr":..,"pt":..}',
    logo        VARCHAR(1024)    NULL COMMENT '品牌 logo 地址',
    sort        INT          NOT NULL DEFAULT 0,
    is_show     TINYINT(1)   NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外商品品牌';

ALTER TABLE eb_overseas_product
    ADD COLUMN brand_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '品牌 id（eb_overseas_brand.id），空 = 无品牌'
    AFTER category_id;
