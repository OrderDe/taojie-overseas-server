-- Migration for databases created before the FX price calculator landed.
-- Run once against an existing overseas database:
--   mysql -u root -p taojie_overseas < sql/fx-rates.sql
-- Safe to re-run: CREATE TABLE IF NOT EXISTS makes a second run a no-op.
-- Table shape matches schema.sql (eb_overseas_fx_rate); the Node backend
-- keeps the same cache in its fx_rates table.

CREATE TABLE IF NOT EXISTS eb_overseas_fx_rate (
    base              VARCHAR(3)  NOT NULL COMMENT '基准币种代码，如 CNY',
    rates             TEXT        NOT NULL COMMENT '汇率表 JSON {代码: 每 1 基准币可换数量}',
    source_updated_at VARCHAR(40) NOT NULL DEFAULT '' COMMENT '数据源更新时间（ISO-8601）',
    fetched_at        VARCHAR(40) NOT NULL DEFAULT '' COMMENT '我方拉取时间（ISO-8601）',
    PRIMARY KEY (base)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '汇率缓存（后台试算工具）';
