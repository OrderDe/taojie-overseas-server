-- Migration for already-deployed databases: admin accounts (user login +
-- role menu permissions). Fresh installs get the same objects from
-- schema.sql. Safe to re-run (IF NOT EXISTS / INSERT IGNORE).
--   mysql -u root -p taojie_overseas < sql/admin-rbac.sql
-- After applying, restart the app: on first boot with an empty user table
-- it creates the initial account  admin / <ADMIN_TOKEN>  (super-admin role).

USE taojie_overseas;

CREATE TABLE IF NOT EXISTS eb_overseas_admin_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(64)  NOT NULL,
    permissions JSON         NOT NULL COMMENT '菜单权限数组，如 ["products","rfq"] 或 ["*"]',
    builtin     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '内置角色（超级管理员）不可删',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外后台角色';

CREATE TABLE IF NOT EXISTS eb_overseas_admin_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL COMMENT 'PBKDF2: pbkdf2:迭代:盐:哈希',
    display_name  VARCHAR(64)      NULL,
    role_id       BIGINT       NOT NULL,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外后台用户';

CREATE TABLE IF NOT EXISTS eb_overseas_admin_session (
    token       VARCHAR(128) NOT NULL,
    user_id     BIGINT       NOT NULL,
    expires_at  BIGINT       NOT NULL COMMENT '毫秒时间戳',
    PRIMARY KEY (token),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外后台登录会话';

INSERT IGNORE INTO eb_overseas_admin_role (name, permissions, builtin) VALUES
    ('超级管理员', '["*"]', 1),
    ('运营', '["products", "categories", "rfq", "chat", "config"]', 0),
    ('外贸员', '["rfq", "chat"]', 0);
