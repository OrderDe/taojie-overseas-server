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

CREATE TABLE IF NOT EXISTS eb_overseas_product (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    source_id   VARCHAR(64)  NOT NULL COMMENT 'tjmall 商品 id（同步 upsert 键）',
    category_id VARCHAR(64)  NOT NULL,
    brand_id    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '品牌 id（eb_overseas_brand.id），空 = 无品牌',
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

-- 后台账号体系（用户登录 + 角色菜单权限）。已部署的库改跑 sql/admin-rbac.sql。
-- 角色 permissions 是菜单权限 key 的 JSON 数组：products/categories/rfq/config/users，["*"] = 全部。
-- 初始管理员账号不写在这里：应用启动时若用户表为空，用 ADMIN_TOKEN 作为密码创建 admin 账号。
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

-- 在线聊天（访客端悬浮聊天框 + 后台实时查看/回复）。已部署的库改跑 sql/chat.sql。
-- 访客身份只靠随机 token（浏览器 localStorage 保存），不建账号；
-- last_agent_read_msg_id 是客服已读水位线，会话列表的未读数由此算出。
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
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    session_id  BIGINT   NOT NULL,
    sender      VARCHAR(16) NOT NULL COMMENT 'visitor 访客 / agent 客服',
    body        TEXT     NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session (session_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '海外在线聊天消息';
