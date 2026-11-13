-- Migration for already-deployed databases: custom pages
-- (admin-authored standalone pages the storefront nav can point at:
-- public GET /api/pages[/:slug], admin CRUD under /api/admin/pages,
-- perm "config" — the role that already holds "config" gains the menu
-- automatically, no role changes needed). Fresh installs get the same
-- table from schema.sql. Safe to re-run (IF NOT EXISTS).
--   mysql -u root -p taojie_overseas < sql/custom-pages.sql
-- The sample "cases" page is seeded by the service itself on first
-- boot while the table is empty (same content as the Node backend).

USE taojie_overseas;

CREATE TABLE IF NOT EXISTS eb_overseas_custom_page (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    slug        VARCHAR(64)  NOT NULL COMMENT '页面路径标识 ^[a-z0-9][a-z0-9-]{1,60}$，导航按它引用页面',
    titles      JSON         NOT NULL COMMENT '多语言页面标题 {"en":..,"zh":..,...}',
    sections    JSON         NOT NULL COMMENT '内容区块 [{title:{..},body:{..},image}]',
    gallery     JSON         NOT NULL COMMENT '图库 URL 数组',
    sort        INT          NOT NULL DEFAULT 0,
    is_show     TINYINT(1)   NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_page_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外自定义页面（后台图文页，导航可指向）';
