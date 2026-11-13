-- Migration for already-deployed databases: paint formula library
-- (storefront 配方搜索 / 公司简介 pages + admin 配方管理 menus), replicated
-- from the sfsy (JBolt) tinting system — same tables/columns as the
-- source schema, with the eb_overseas_ prefix. Fresh installs get the
-- same tables from schema.sql. Safe to re-run (IF NOT EXISTS; the role
-- UPDATE below simply re-applies the shipped default — see the note
-- before running it if you customised the 运营 role).
--   mysql -u root -p taojie_overseas < sql/formula.sql
-- The data itself is imported separately from the sfsy dump
-- (MySQL: load the source dump after renaming
-- the tables with the eb_overseas_ prefix).

USE taojie_overseas;

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

-- Give the shipped 运营 role the new "formula" menu permission.
-- NOTE: this UPDATE resets 运营 to the shipped default. If you customised
-- that role in the admin UI, skip the UPDATE and instead add "formula"
-- to it under 用户与角色 after restarting the app. 外贸员 does not get
-- the formula permission by default (same as the Node backend).
UPDATE eb_overseas_admin_role
SET permissions = '["products", "categories", "rfq", "chat", "config", "visitors", "formula"]'
WHERE name = '运营';
