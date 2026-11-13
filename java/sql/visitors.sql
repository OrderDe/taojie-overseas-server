-- Migration for already-deployed databases: visitor statistics
-- (storefront /api/track beacon + admin 访客统计 page). Fresh installs
-- get the same objects from schema.sql. Safe to re-run (IF NOT EXISTS;
-- the role UPDATEs simply re-apply the shipped defaults — see the note
-- below before running them if you customised the 运营 / 外贸员 roles).
--   mysql -u root -p taojie_overseas < sql/visitors.sql

USE taojie_overseas;

CREATE TABLE IF NOT EXISTS eb_overseas_visitor (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    token            VARCHAR(128) NOT NULL COMMENT '访客随机 token（唯一身份）',
    ip               VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '访客 IP（x-forwarded-for 优先）',
    country          VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'ip-api 地区解析：国家（内网记「本地/内网」）',
    region           VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '省/州',
    city             VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '城市',
    timezone         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '访客时区',
    device           VARCHAR(16)  NOT NULL DEFAULT '' COMMENT 'desktop/mobile/tablet（UA 解析）',
    browser          VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '浏览器（UA 解析）',
    os               VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '操作系统（UA 解析）',
    is_bot           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'UA 疑似机器人（仅信号，不拦截）',
    referrer         VARCHAR(512) NOT NULL DEFAULT '' COMMENT '来源页 document.referrer',
    first_page       VARCHAR(512) NOT NULL DEFAULT '' COMMENT '首次落地页标签',
    current_page     VARCHAR(512) NOT NULL DEFAULT '' COMMENT '当前页标签',
    page_views       INT          NOT NULL DEFAULT 0 COMMENT '累计浏览页数（只数 view）',
    visits           INT          NOT NULL DEFAULT 0 COMMENT '来访次数（间隔超 30 分钟算新一次）',
    first_seen       DATETIME     NULL COMMENT '首次访问时间',
    last_seen        DATETIME     NULL COMMENT '最近活动时间，60 秒内视为在线',
    visit_started_at DATETIME     NULL COMMENT '本次访问开始时间（新访问时重置）',
    dwell_sec        INT          NOT NULL DEFAULT 0 COMMENT '累计停留秒数（单步增量封顶 90 秒）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token),
    KEY idx_last_seen (last_seen)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外访客统计（访客行）';

CREATE TABLE IF NOT EXISTS eb_overseas_visit_event (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    token       VARCHAR(128) NOT NULL COMMENT '访客 token',
    kind        VARCHAR(16)  NOT NULL DEFAULT 'view' COMMENT 'view 浏览 / click 点击',
    page        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '事件发生时的页标签',
    action      VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'click 的描述（如 点商品:1K141），view 为空',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_token (token, id),
    KEY idx_kind_time (kind, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外访客行程事件';

-- Give the shipped non-super roles the new "visitors" menu permission.
-- NOTE: these UPDATEs reset 运营 / 外贸员 to the shipped defaults. If you
-- customised those roles in the admin UI, skip the UPDATEs and instead add
-- "visitors" to them under 用户与角色 after restarting the app.
UPDATE eb_overseas_admin_role
SET permissions = '["products", "categories", "rfq", "chat", "config", "visitors"]'
WHERE name = '运营';
UPDATE eb_overseas_admin_role
SET permissions = '["rfq", "chat", "visitors"]'
WHERE name = '外贸员';
