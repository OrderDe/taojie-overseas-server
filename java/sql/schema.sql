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
    parent_id   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '父级分类 id，空 = 一级分类（三级分类，见 category-hierarchy.sql）',
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
    negotiable  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否面议（1 = 前台显示面议、不显示数字价、不可加购）',
    sales       INT          NOT NULL DEFAULT 0,
    unit        VARCHAR(64)      NULL,
    moq         INT          NOT NULL DEFAULT 1,
    image       VARCHAR(1024)    NULL,
    video       VARCHAR(1024)    NULL COMMENT '商品视频 URL（空 = 无视频）',
    tag         VARCHAR(128)     NULL,
    blurb       JSON             NULL COMMENT '多语言简介（也可存单语言纯文本）',
    specs       JSON             NULL COMMENT '参数 {"Mix ratio":"2 : 1",...}',
    localized_content JSON       NULL COMMENT 'Localized unit, tag and specs',
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

-- 汇率缓存（后台「汇率换算」试算工具）。已部署的库改跑 sql/fx-rates.sql。
-- 数据源 open.er-api.com，按 base 一行，12 小时内直接用缓存。
CREATE TABLE IF NOT EXISTS eb_overseas_fx_rate (
    base              VARCHAR(3)  NOT NULL COMMENT '基准币种代码，如 CNY',
    rates             TEXT        NOT NULL COMMENT '汇率表 JSON {代码: 每 1 基准币可换数量}',
    source_updated_at VARCHAR(40) NOT NULL DEFAULT '' COMMENT '数据源更新时间（ISO-8601）',
    fetched_at        VARCHAR(40) NOT NULL DEFAULT '' COMMENT '我方拉取时间（ISO-8601）',
    PRIMARY KEY (base)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '汇率缓存（后台试算工具）';

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
    ip                     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '访客 IP（x-forwarded-for 优先）',
    country                VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'ip-api 地区解析：国家（内网记「本地/内网」）',
    region                 VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '省/州',
    city                   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '城市',
    timezone               VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '访客时区',
    device                 VARCHAR(16)  NOT NULL DEFAULT '' COMMENT 'desktop/mobile/tablet（UA 解析）',
    browser                VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '浏览器（UA 解析）',
    os                     VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '操作系统（UA 解析）',
    referrer               VARCHAR(512) NOT NULL DEFAULT '' COMMENT '来源页 document.referrer',
    current_page           VARCHAR(512) NOT NULL DEFAULT '' COMMENT '当前浏览页面：路径 · 标题',
    page_views             INT          NOT NULL DEFAULT 0 COMMENT '会话内浏览页面数',
    last_seen_at           DATETIME     NULL COMMENT '最后心跳/发消息时间，45 秒内视为在线',
    suspect_bot            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'UA 疑似机器人（仅信号，不拦截）',
    visitor_typing_at      DATETIME     NULL COMMENT '访客正在输入时间戳',
    agent_typing_at        DATETIME     NULL COMMENT '客服正在输入时间戳',
    visitor_read_msg_id    BIGINT       NOT NULL DEFAULT 0 COMMENT '访客已读到的最大客服消息 id',
    note                   VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '内部备注（访客不可见）',
    status                 VARCHAR(16)  NOT NULL DEFAULT 'open' COMMENT 'open/closed，访客发消息自动重开',
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

-- 访客统计（全站上报 /api/track + 后台「访客统计」页）。已部署的库改跑 sql/visitors.sql。
-- 与聊天分开：聊天只记开过聊天框的人，这两张表记所有访客（一行一个 token）
-- 和逐条浏览/点击事件（PV/UV/趋势只数 kind='view'，点击不算浏览量）。
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

-- 配方库（从 sfsy/JBolt 调色系统复制：前台「配方搜索/公司简介」+ 后台「配方管理」）。
-- 表名/列名与源系统一致，加 eb_overseas_ 前缀；已部署的库改跑 sql/formula.sql。
-- 关系表已用真实数据验证：relation.car_color = car_color.id、
-- relation.color_number = color_number.id、color_formula.colornum_id = color_number.id。
CREATE TABLE IF NOT EXISTS eb_overseas_car_brand (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255) NULL COMMENT '品牌',
    name_first    VARCHAR(2)   NULL COMMENT '首字母',
    country       VARCHAR(50)  NULL COMMENT '国家',
    url           VARCHAR(255) NULL COMMENT 'url',
    letter_rank   INT          NULL COMMENT '排序',
    enable        CHAR(1)      NULL DEFAULT '1' COMMENT '是否启用',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_time   DATETIME     NULL COMMENT '更新时间',
    create_user_id VARCHAR(255) NULL COMMENT '创建人',
    update_user_id VARCHAR(255) NULL COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_name (name, name_first)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '汽车品牌（配方库）';

CREATE TABLE IF NOT EXISTS eb_overseas_car_model (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    brand         VARCHAR(32)  NULL COMMENT '品牌',
    brand_id      BIGINT       NULL COMMENT '品牌ID',
    model         VARCHAR(32)  NOT NULL COMMENT '型号',
    sort_rank     INT          NULL COMMENT '排序',
    enable        CHAR(1)      NULL DEFAULT '1' COMMENT '是否启用',
    update_time   DATETIME     NULL COMMENT '更新时间',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    create_time   DATETIME     NULL COMMENT '创建时间',
    create_user_id VARCHAR(200) NULL COMMENT '创建人',
    update_user_id VARCHAR(200) NULL COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_brand (brand_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '车型库';

CREATE TABLE IF NOT EXISTS eb_overseas_car_country (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50) NULL COMMENT '国家',
    name_en    VARCHAR(50) NULL COMMENT '英文名称',
    language   VARCHAR(20) NULL COMMENT '语言',
    code       INT         NULL COMMENT '编码',
    state      INT         NULL COMMENT '状态',
    enable     CHAR(1)     NULL COMMENT '是否启用',
    sort_rank  INT         NULL COMMENT '排序',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '国家信息';

CREATE TABLE IF NOT EXISTS eb_overseas_car_factory (
    id         BIGINT       NOT NULL,
    name       VARCHAR(255) NULL COMMENT '生产厂',
    brand_id   BIGINT       NULL COMMENT '品牌',
    country    VARCHAR(255) NULL COMMENT '国家',
    state      INT          NULL COMMENT '状态',
    enable     CHAR(1)      NULL COMMENT '是否启用',
    sort_rank  INT          NULL COMMENT '排序',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '汽车生产商';

CREATE TABLE IF NOT EXISTS eb_overseas_color_card (
    id            BIGINT       NOT NULL,
    name          VARCHAR(255) NULL COMMENT '名称',
    code          VARCHAR(50)  NULL COMMENT '编码',
    enable        CHAR(1)      NULL COMMENT '是否启用',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_time   DATETIME     NULL COMMENT '更新时间',
    create_user_id VARCHAR(255) NULL COMMENT '创建人',
    update_user_id VARCHAR(255) NULL COMMENT '更新人',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '色卡';

CREATE TABLE IF NOT EXISTS eb_overseas_color_masterbatch (
    id            BIGINT       NOT NULL,
    model         VARCHAR(32)  NOT NULL COMMENT '色母型号',
    name          VARCHAR(128) NOT NULL COMMENT '名称',
    eng_name      VARCHAR(255) NOT NULL COMMENT '英文名称',
    type          VARCHAR(32)  NULL COMMENT '色母类型',
    mark          VARCHAR(64)  NULL COMMENT '备注',
    enable        CHAR(1)      NULL COMMENT '是否启用',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_time   DATETIME     NULL COMMENT '更新时间',
    create_user_id VARCHAR(200) NULL COMMENT '创建人',
    update_user_id VARCHAR(200) NULL COMMENT '更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_model (model)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '色母表';

CREATE TABLE IF NOT EXISTS eb_overseas_car_color (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    brand_id      BIGINT       NOT NULL COMMENT '品牌id',
    model_id      BIGINT       NOT NULL COMMENT '车型id',
    car_color     VARCHAR(255) NULL COMMENT '车辆颜色',
    factory_color VARCHAR(50)  NULL COMMENT '出厂色号',
    color_num     VARCHAR(50)  NULL COMMENT '色号',
    card_num      VARCHAR(50)  NULL COMMENT '色卡编号',
    car_image     VARCHAR(255) NULL DEFAULT 'https://cdn.tujia.club/tujia/car/car.jpg' COMMENT '车辆原图',
    enable        CHAR(1)      NULL DEFAULT '1' COMMENT '是否启用',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_time   DATETIME     NULL COMMENT '更新时间',
    create_user_id VARCHAR(200) NULL COMMENT '创建人',
    update_user_id VARCHAR(200) NULL COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_brand_model (brand_id, model_id),
    KEY idx_color_num (color_num)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '车辆颜色';

CREATE TABLE IF NOT EXISTS eb_overseas_car_color0 (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    brand_id      BIGINT       NOT NULL COMMENT '品牌id',
    model_id      BIGINT       NOT NULL COMMENT '车型id',
    car_color     VARCHAR(255) NULL COMMENT '车辆颜色',
    factory_color VARCHAR(50)  NULL COMMENT '出厂色号',
    color_num     VARCHAR(50)  NULL COMMENT '色号',
    card_num      VARCHAR(50)  NULL COMMENT '色卡编号',
    car_image     VARCHAR(255) NULL DEFAULT 'https://cdn.tujia.club/tujia/car/car.jpg' COMMENT '车辆原图',
    enable        CHAR(1)      NULL DEFAULT '1' COMMENT '是否启用',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_time   DATETIME     NULL COMMENT '更新时间',
    create_user_id VARCHAR(200) NULL COMMENT '创建人',
    update_user_id VARCHAR(200) NULL COMMENT '更新人',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '车辆颜色（编号变体，同步源表）';

CREATE TABLE IF NOT EXISTS eb_overseas_car_color_bh (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    brand_id      BIGINT       NULL COMMENT '品牌id',
    model_id      BIGINT       NULL COMMENT '车型id',
    car_color     VARCHAR(255) NULL COMMENT '车辆颜色',
    factory_color VARCHAR(50)  NULL COMMENT '出厂色号',
    innercode     VARCHAR(50)  NULL COMMENT '内部色号',
    description   VARCHAR(50)  NULL COMMENT '描述',
    car_image     VARCHAR(255) NULL COMMENT '车辆原图',
    enable        CHAR(1)      NULL DEFAULT '1' COMMENT '是否启用',
    year          VARCHAR(200) NULL DEFAULT '0' COMMENT '年份',
    devcount      VARCHAR(200) NULL COMMENT 'devcount',
    devgroup      VARCHAR(200) NULL COMMENT 'devgroup',
    colorcode     VARCHAR(200) NULL COMMENT 'colorcode',
    innergroup    VARCHAR(200) NULL COMMENT 'innergroup',
    formulas      VARCHAR(255) NULL COMMENT 'formulas',
    devsite       VARCHAR(255) NULL COMMENT 'devsite',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '车辆颜色（bh 变体，同步源表）';

CREATE TABLE IF NOT EXISTS eb_overseas_color_formula (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    colornum_id   BIGINT       NOT NULL COMMENT '色号id',
    name          VARCHAR(32)  NULL COMMENT '色母名称',
    model         VARCHAR(32)  NOT NULL COMMENT '色母型号',
    dose          DOUBLE       NOT NULL COMMENT '剂量',
    color_rank    INT          NOT NULL COMMENT '顺序',
    enable        CHAR(1)      NULL COMMENT '是否生效',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_time   DATETIME     NULL COMMENT '更新时间',
    create_user_id VARCHAR(200) NULL COMMENT '创建人',
    update_user_id VARCHAR(200) NULL COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_colornum (colornum_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '颜色配方表';

CREATE TABLE IF NOT EXISTS eb_overseas_color_formula_bh (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    innergroup    VARCHAR(32)  NOT NULL COMMENT '色号组',
    fname         VARCHAR(200) NULL COMMENT '色母名称',
    fdesc         VARCHAR(200) NULL COMMENT '色母型号',
    lalm          DOUBLE       NULL COMMENT '剂量',
    ordeid        INT          NULL COMMENT '顺序',
    enable        CHAR(1)      NULL COMMENT '是否生效',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_time   DATETIME     NULL COMMENT '更新时间',
    create_user_id VARCHAR(200) NULL COMMENT '创建人',
    update_user_id VARCHAR(200) NULL COMMENT '更新人',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '颜色配方表（bh 变体，同步源表）';

CREATE TABLE IF NOT EXISTS eb_overseas_color_number (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(32)  NOT NULL COMMENT '色号',
    color_name    VARCHAR(255) NULL COMMENT '颜色名称',
    lab_l         DOUBLE       NULL COMMENT 'LAB_L',
    lab_a         DOUBLE       NULL COMMENT 'LAB_A',
    lab_b         DOUBLE       NULL COMMENT 'LAB_B',
    enable        CHAR(1)      NULL DEFAULT '1' COMMENT '是否启用',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_time   DATETIME     NULL COMMENT '更新时间',
    create_user_id VARCHAR(200) NULL COMMENT '创建人',
    update_user_id VARCHAR(200) NULL COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '色号表';

CREATE TABLE IF NOT EXISTS eb_overseas_color_number_relation (
    id           BIGINT  NOT NULL AUTO_INCREMENT,
    car_color    BIGINT  NULL COMMENT '车辆颜色id（= car_color.id）',
    color_number BIGINT  NULL COMMENT '色号id（= color_number.id）',
    enable       CHAR(1) NULL DEFAULT '1' COMMENT '有效',
    PRIMARY KEY (id),
    KEY idx_car_color (car_color),
    KEY idx_color_number (color_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '颜色色号关系表';

CREATE TABLE IF NOT EXISTS eb_overseas_color_brand (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255) NULL COMMENT '品牌',
    logo          VARCHAR(255) NULL COMMENT 'logo',
    enable        CHAR(1)      NULL DEFAULT '1' COMMENT '是否启用',
    update_time   DATETIME     NULL COMMENT '更新时间',
    create_time   DATETIME     NULL COMMENT '创建时间',
    create_user_id VARCHAR(255) NULL COMMENT '创建人',
    update_user_id VARCHAR(255) NULL COMMENT '更新人',
    is_deleted    CHAR(1)      NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '产品品牌';

CREATE TABLE IF NOT EXISTS eb_overseas_color_collection (
    id            INT          NOT NULL,
    wechat_user_id INT         NOT NULL COMMENT '微信用户id',
    nickname      VARCHAR(255) NULL COMMENT '用户昵称',
    model_id      INT          NULL COMMENT '收藏车型配方',
    color_id      INT          NULL COMMENT '颜色Id',
    brand_id      INT          NULL COMMENT '品牌Id',
    mp_id         INT          NULL COMMENT '微信小程序Id',
    create_time   DATETIME     NULL COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '配方收藏信息（同步源表）';
