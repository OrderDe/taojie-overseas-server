-- Migration for already-deployed databases: online chat (storefront chat
-- widget + admin live view/reply). Fresh installs get the same objects
-- from schema.sql. Safe to re-run (IF NOT EXISTS; the role UPDATEs simply
-- re-apply the shipped defaults — see the note below before running them
-- if you customised the 运营 / 外贸员 roles).
--   mysql -u root -p taojie_overseas < sql/chat.sql

USE taojie_overseas;

CREATE TABLE IF NOT EXISTS eb_overseas_chat_session (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    visitor_token          VARCHAR(128) NOT NULL COMMENT '访客随机 token（唯一身份）',
    visitor_name           VARCHAR(64)  NOT NULL DEFAULT '访客',
    lang                   VARCHAR(8)   NOT NULL DEFAULT 'en',
    last_agent_read_msg_id BIGINT       NOT NULL DEFAULT 0 COMMENT '客服已读到的最大消息 id',
    last_msg_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_visitor_token (visitor_token),
    KEY idx_last_msg_at (last_msg_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外在线聊天会话';

CREATE TABLE IF NOT EXISTS eb_overseas_chat_message (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    session_id  BIGINT      NOT NULL,
    sender      VARCHAR(16) NOT NULL COMMENT 'visitor 访客 / agent 客服',
    body        TEXT        NOT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session (session_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外在线聊天消息';

-- Chat enhancement (visitor profile / presence / typing / read receipts /
-- note / status) for databases whose chat tables already exist from the
-- first chat release. Fresh installs get these columns from the CREATE
-- TABLE in schema.sql (and from the CREATE above on a truly empty DB the
-- columns below are what bring it up to date). Re-running reports
-- "Duplicate column name ..." for columns already applied — that error is
-- expected on a second run and means there is nothing to do; run the
-- statements one by one and skip the ones that report it.
ALTER TABLE eb_overseas_chat_session ADD COLUMN ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '访客 IP（x-forwarded-for 优先）';
ALTER TABLE eb_overseas_chat_session ADD COLUMN country VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'ip-api 地区解析：国家（内网记「本地/内网」）';
ALTER TABLE eb_overseas_chat_session ADD COLUMN region VARCHAR(64) NOT NULL DEFAULT '' COMMENT '省/州';
ALTER TABLE eb_overseas_chat_session ADD COLUMN city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '城市';
ALTER TABLE eb_overseas_chat_session ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT '' COMMENT '访客时区';
ALTER TABLE eb_overseas_chat_session ADD COLUMN device VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'desktop/mobile/tablet（UA 解析）';
ALTER TABLE eb_overseas_chat_session ADD COLUMN browser VARCHAR(32) NOT NULL DEFAULT '' COMMENT '浏览器（UA 解析）';
ALTER TABLE eb_overseas_chat_session ADD COLUMN os VARCHAR(32) NOT NULL DEFAULT '' COMMENT '操作系统（UA 解析）';
ALTER TABLE eb_overseas_chat_session ADD COLUMN referrer VARCHAR(512) NOT NULL DEFAULT '' COMMENT '来源页 document.referrer';
ALTER TABLE eb_overseas_chat_session ADD COLUMN current_page VARCHAR(512) NOT NULL DEFAULT '' COMMENT '当前浏览页面：路径 · 标题';
ALTER TABLE eb_overseas_chat_session ADD COLUMN page_views INT NOT NULL DEFAULT 0 COMMENT '会话内浏览页面数';
ALTER TABLE eb_overseas_chat_session ADD COLUMN last_seen_at DATETIME NULL COMMENT '最后心跳/发消息时间，45 秒内视为在线';
ALTER TABLE eb_overseas_chat_session ADD COLUMN suspect_bot TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'UA 疑似机器人（仅信号，不拦截）';
ALTER TABLE eb_overseas_chat_session ADD COLUMN visitor_typing_at DATETIME NULL COMMENT '访客正在输入时间戳';
ALTER TABLE eb_overseas_chat_session ADD COLUMN agent_typing_at DATETIME NULL COMMENT '客服正在输入时间戳';
ALTER TABLE eb_overseas_chat_session ADD COLUMN visitor_read_msg_id BIGINT NOT NULL DEFAULT 0 COMMENT '访客已读到的最大客服消息 id';
ALTER TABLE eb_overseas_chat_session ADD COLUMN note VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '内部备注（访客不可见）';
ALTER TABLE eb_overseas_chat_session ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'open' COMMENT 'open/closed，访客发消息自动重开';

-- Give the shipped non-super roles the new "chat" menu permission.
-- NOTE: these UPDATEs reset 运营 / 外贸员 to the shipped defaults. If you
-- customised those roles in the admin UI, skip the UPDATEs and instead add
-- "chat" to them under 用户与角色 after restarting the app.
UPDATE eb_overseas_admin_role
SET permissions = '["products", "categories", "rfq", "chat", "config"]'
WHERE name = '运营';
UPDATE eb_overseas_admin_role
SET permissions = '["rfq", "chat"]'
WHERE name = '外贸员';
