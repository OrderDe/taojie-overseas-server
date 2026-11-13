# taojie-overseas-server

海外商城后端，使用 **Java 17 + Spring Boot + MyBatis-Plus + MySQL 8**。
Node/SQLite 版及内置旧后台已移除；管理界面使用独立仓库 `taojie-overseas-admin`，商城前台使用 `taojie-pc-overseas`。

## 目录

- `java/`：后端源码、Maven 构建、SQL 建表与升级脚本、MySQL 商品导入工具。
- `data/`：商品与配方原始数据，保留用于导入及恢复；拉取代码不会自动写入 MySQL。
- `docker-compose.yml`：单独启动 LibreTranslate 翻译服务。
- `.env.example`：环境变量示例；Java 进程需由启动终端或服务管理器注入变量。

## 运行

1. 按 [Java 文档](java/README.md) 建立 MySQL 数据库，并执行需要的 SQL 迁移。现有数据库升级前先备份，不要重复导入演示数据。
2. 在 `java/` 下运行 `mvn spring-boot:run`，或执行 `mvn package` 后运行生成的 JAR。
3. 当前默认 profile 为 dev；通过 `SPRING_PROFILES_ACTIVE` 或 `--spring.profiles.active=prod` 切换环境。数据库地址和库名见对应的 `application-*.yml`，密码通过 `DB_PASSWORD` 注入。
4. 服务默认端口 3100，接口文档为 `/swagger-ui.html`。前台和独立后台将 `/api/` 代理到该端口。

首次用户表为空时，设置 `ADMIN_TOKEN` 可初始化 admin 账号；日常使用后台管理的账号与角色。数据库、OSS 密钥和真实令牌不要提交仓库。OSS 配置参考 `java/oss.example.properties`，通过外部配置文件注入。

## 自动翻译

运行 `docker compose up -d libretranslate`。服务仅绑定 `127.0.0.1:5000`，Java 使用 `LIBRETRANSLATE_URL` 连接；可选 `LIBRETRANSLATE_API_KEY`。
翻译模型首次下载可能需要较长时间。也可显式设置 `TRANSLATE_PROVIDER=mymemory` 使用 MyMemory；可选 `TRANSLATE_EMAIL`，不会静默切换供应商。

## 数据导入

MySQL 商品导入入口为 `java/tools/import-products.py`，格式及用法见 [Java 文档](java/README.md)。
配方快照保存在 `data/sfsy_data.sql.gz.b64`；执行 MySQL 导入前需要解码、解压并按 `java/sql/formula.sql` 说明核对表名。数据源保留不代表已自动同步到数据库。
