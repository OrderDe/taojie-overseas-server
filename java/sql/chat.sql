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
