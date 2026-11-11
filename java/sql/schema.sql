-- Taojie overseas mall — MySQL 8 schema.
-- Table prefix eb_ follows tjmall's naming (eb_store_product, eb_system_config, ...).
-- Apply once on the Jakarta MySQL instance:
--   mysql -u root -p taojie_overseas < sql/schema.sql
-- Then load the demo catalogue (same 14 products as the Node service):
--   mysql -u root -p taojie_overseas < sql/seed.sql

CREATE DATABASE IF NOT EXISTS taojie_overseas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE taojie_overseas;

CREATE TABLE IF NOT EXISTS eb_overseas_category (
    id          VARCHAR(64)  NOT NULL COMMENT '分类 id（与前端/种子数据一致，如 clear、paint）',
    names       JSON         NOT NULL COMMENT '多语言名称 {"en":..,"zh":..,"es":..,"fr":..,"pt":..}',
    sort        INT          NOT NULL DEFAULT 0,
    is_show     TINYINT(1)   NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外商品分类';

CREATE TABLE IF NOT EXISTS eb_overseas_product (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    source_id   VARCHAR(64)  NOT NULL COMMENT 'tjmall 商品 id（同步 upsert 键）',
    category_id VARCHAR(64)  NOT NULL,
    names       JSON         NOT NULL COMMENT '多语言名称 {"en":..,"zh":..,...}',
    price       DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '美元价',
    ot_price    DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '美元原价',
    sales       INT          NOT NULL DEFAULT 0,
    unit        VARCHAR(64)      NULL,
    moq         INT          NOT NULL DEFAULT 1,
    image       VARCHAR(1024)    NULL,
    tag         VARCHAR(128)     NULL,
    blurb       JSON             NULL COMMENT '多语言简介（也可存单语言纯文本）',
    specs       JSON             NULL COMMENT '参数 {"Mix ratio":"2 : 1",...}',
    is_show     TINYINT(1)   NOT NULL DEFAULT 1,
    sort        INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_id (source_id),
    KEY idx_category (category_id, is_show)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外商品（tjmall 同步副本 + 海外字段）';

CREATE TABLE IF NOT EXISTS eb_overseas_rfq (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(128) NOT NULL,
    company     VARCHAR(255)     NULL,
    country     VARCHAR(128)     NULL,
    contact     VARCHAR(255) NOT NULL COMMENT '邮箱/WhatsApp/电话',
    product_name VARCHAR(255)    NULL,
    quantity    VARCHAR(128)     NULL,
    message     TEXT             NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外询价留资（一期只存海外库）';

CREATE TABLE IF NOT EXISTS eb_overseas_site_config (
    config_key   VARCHAR(64)  NOT NULL COMMENT '与 pc-config.json 字段同名',
    config_value VARCHAR(1024)    NULL,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外站点配置（键值表，同 eb_system_config 模式）';
