# tjMall-overseas（海外后端 · Java 版）

按 **tjmall 的框架** 设计的海外 PC 商城后端。tjmall（`com.zbkj`，ZBKJ/CRMEB 血统）
是 Java 17 + Spring Boot 3.3.13 + Spring Cloud Alibaba 微服务全家桶；海外站只是一个
公开展示 + 询价服务，用不上网关/Nacos/搜索/工作流那一整套，所以这里取 tjmall 的
**框架约定**、砍掉它的**部署体量**：单模块 Spring Boot 应用，规范与 tjmall 完全同源。

## 与 tjmall 的对照

| tjmall | 本项目 | 说明 |
| --- | --- | --- |
| 根 pom：Java 17 / Spring Boot 3.3.13 / MyBatis-Plus 3.5.7 / PageHelper 2.1.0 / MySQL 8.4.0 / Hutool 5.8.35 / Lombok 1.18.36 | `pom.xml` 逐项对齐 | 同版本，团队习惯不变 |
| `tjMall-common` 的 `common/result/CommonResult` `{code, message, data}` | `common/result/CommonResult` 同形实现 | 前端解包方式与小程序端一致 |
| `common/result/*ResultCode` 按域拆分（Product/Order/...） | `CommonResultCode` + `OverseasResultCode` | 海外域错误码 6001–6004 |
| `common/page/CommonPage`（由 PageHelper `PageInfo` 构建） | `common/page/CommonPage` 同形 | 分页返回 `{page, limit, totalPage, total, list}` |
| `common/exception` 全局异常处理 | `common/exception`（`OverseasException` + 全局处理器） | 异常一律出 CommonResult |
| `tjMall-app` 的 `front/controller|request|response` 分包 | `controller/`、`request/`、`response/` 同构 | 控制器风格照 `ProductCategoryController`：`@RestController` + Swagger `@Tag/@Operation` + `@PermitAll` |
| `tjMall-service` 的 `service + service/impl(extends ServiceImpl) + mapper(BaseMapper)` | `service/`、`service/impl/`、`dao/`（BaseMapper） | 三层结构与命名习惯一致 |
| 表前缀 `eb_`（`eb_store_product`、`eb_system_config`） | `eb_overseas_product`、`eb_overseas_category`、`eb_overseas_rfq`、`eb_overseas_site_config` | 站点配置用键值表，同 `eb_system_config` 模式 |
| `sql/` 目录放建表脚本 | `sql/schema.sql` + `sql/seed.sql` | 手工执行，同 tjmall 运维习惯 |
| 未照搬：gateway / Nacos / Redis 集群 / tjMall-search / flowable / flink / log | — | 海外站不需要；询价限流先用内存计数，真需要时按同接口换 Redis |

与已上线仓库根目录的 Node 版（`src/server.js`）关系：**接口契约完全一致**
（路径、字段、`/api` 前缀、3100 端口），Java 版是它的正式替代实现，前端零改动。
数据仍是各存各的：本服务连雅加达自己的 MySQL，不直连 tjmall 国内库；
商品通过导入脚本从 tjmall 导出数据同步进来（见下）。

## 接口一览

公开（匿名可访问，与 PC 前端现有调用一一对应）：

```
GET  /api/health
GET  /api/categories?lang=en
GET  /api/products?lang=&keyword=&categoryId=&page=&limit=     -> CommonPage
GET  /api/products/{id}?lang=
GET  /api/keywords?lang=
GET  /api/site-config
POST /api/rfq            { name*, contact*, company, country, productName, quantity, message }
```

管理（请求头 `x-admin-token: <ADMIN_TOKEN>`，拦截器 `AdminTokenInterceptor`）：

```
GET  /api/admin/rfq                 最新 200 条询价
PUT  /api/admin/site-config         保存站点配置（字段同 pc-config.json）
```

启动后 Swagger 文档在 `/swagger-ui.html`。

## 本地运行

要求：JDK 17、Maven 3.9+、MySQL 8。

```bash
# 1. 建库建表 + 演示数据（与 Node 版相同的 14 个示例商品）
mysql -u root -p < sql/schema.sql
mysql -u root -p taojie_overseas < sql/seed.sql

# 2. 启动（环境变量给数据库与管理口令）
DB_PASSWORD=你的密码 ADMIN_TOKEN=你的口令 mvn spring-boot:run
# 或打包：mvn -DskipTests package && java -jar target/tjMall-overseas.jar

# 3. 验证
curl 'http://localhost:3100/api/products?lang=zh&keyword=清漆'
```

## 导入 tjmall 商品

```bash
pip install pymysql
DB_PASSWORD=xxx python3 tools/import-products.py products.json
```

`products.json` 的格式与 Node 版导入脚本相同（一份导出、两边通用）：
`sourceId` 对应 tjmall 商品 id，按它 upsert；英文名/美元价/MOQ 在导出环节补齐。

## 部署（雅加达）

jar + systemd 或 `nohup java -jar` 均可，监听 3100；Nginx 把 `/api/` 反代到
127.0.0.1:3100、前端静态文件走 CDN——与 Node 版的部署方式完全相同，直接替换进程即可。
数据库密码与 ADMIN_TOKEN 只走服务器环境变量，不写进仓库。
