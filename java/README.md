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
| 主 `application.yml` + 按环境拆分，`profiles.active: beta` | `application.yml` + `application-dev/test/beta/prod.yml` 四套 | 环境数与命名和 tjmall 一一对应 |
| MySQL 走 Druid（`DruidDataSource`）+ `infra.host` 统一基础设施地址；各环境库名/端口/池参数不同（dev/beta 同库名、test 独立测试库、prod 大池） | 每套 profile 同构：Druid + `${infra.host}`，库名 `taojie_overseas_dev/_test/_beta` 与正式库 `taojie_overseas` | 照它的环境模型换上海外自己的库 |
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
GET  /api/products?lang=&keyword=&categoryId=&brandId=&page=&limit=     -> CommonPage
GET  /api/products/{id}?lang=
GET  /api/keywords?lang=
GET  /api/site-config
POST /api/rfq            { name*, contact*, company, country, productName, quantity, message }
POST /api/chat/start     { name?, token?, lang?, referrer?, page?, pageTitle? } -> { sessionId, token, name }（带已知 token 则复用会话；服务端解析 UA + ip-api 地区，异步回填不阻塞）
POST /api/chat/messages  { token*, body* } 访客发消息（按 IP 限流；已结束会话自动重开）
GET  /api/chat/messages?token=&afterId=     访客拉本会话消息（afterId 增量；同时推进访客已读水位，返回 agentTyping）
POST /api/chat/ping      { token*, page?, pageTitle? } 心跳：在线状态（45 秒）+ 当前页面
POST /api/chat/typing    { token* } 访客正在输入信号
```

管理（拦截器 `AdminTokenInterceptor`）：先 `POST /api/admin/login` 用账号+密码
登录拿到会话令牌，之后请求头带 `x-admin-token: <会话令牌>`。每个账号有角色，
角色带菜单权限（products/categories/rfq/chat/config/users），接口按区域校验、
没有权限返回 403。`ADMIN_TOKEN` 本身仍可作应急超级令牌（全部权限）。
首次启动种子三个角色（超级管理员/运营/外贸员）；用户表为空时按 `ADMIN_TOKEN`
生成初始账号 **admin**（密码就是该令牌），登录后在「用户与角色」里改密码、加人。
已部署的库先跑 `sql/admin-rbac.sql` 补上三张账号表。

```
POST /api/admin/login               登录（免拦截）{username, password} -> {token, user}
POST /api/admin/logout              退出（作废会话）
GET  /api/admin/me                  当前账号与菜单权限
GET  /api/admin/rfq                 最新 200 条询价（需 rfq 权限）
GET  /api/admin/chat/sessions?keyword=  聊天会话列表：访客名/预览/未读数/访客画像（地区/设备/UA）/在线/疑似机器人（需 chat 权限；keyword 匹配访客名或消息内容）
GET  /api/admin/chat/sessions/{id}/messages  会话全部消息（读取即清未读；session 带 visitorReadMsgId/visitorTyping）
POST /api/admin/chat/sessions/{id}/messages  以客服身份回复 {body}（存量库需跑 sql/chat.sql，增强列在同一文件）
POST /api/admin/chat/sessions/{id}/typing    客服正在输入信号
PUT  /api/admin/chat/sessions/{id}/note      保存内部备注 {note}（访客不可见）
PUT  /api/admin/chat/sessions/{id}/status    结束/重开会话 {status: open|closed}
PUT  /api/admin/site-config         保存站点配置（需 config 权限）
GET|POST /api/admin/products        商品列表（管理版）/ 新建商品（需 products 权限）
PUT|DELETE /api/admin/products/{id} 修改 / 删除商品
GET|POST /api/admin/categories      分类列表（管理版）/ 新建分类（需 categories 权限）
PUT|DELETE /api/admin/categories/{id} 修改 / 删除分类（下有商品时拒绝删除）
GET  /api/brands                     前台品牌列表 [{id, name, logo}]
GET|POST /api/admin/brands          品牌列表（管理版）/ 新建品牌（需 products 权限）
PUT|DELETE /api/admin/brands/{id}   修改 / 删除品牌（下有商品时拒绝删除；存量库需跑 sql/admin-brands.sql）
POST /api/admin/upload              上传商品图 {filename, dataBase64} -> {url}（需 products 权限）
POST /api/admin/translate           名称自动翻译 {text, targets?} -> {translations, errors}（任意登录态；录入时译一次存 names JSON；环境变量同 Node 版：默认自托管 LibreTranslate，LIBRETRANSLATE_URL 默认 http://127.0.0.1:5000；TRANSLATE_PROVIDER=mymemory 才切回 MyMemory，TRANSLATE_EMAIL 提其配额；无静默回退，不支持的语种进 errors）
GET  /api/admin/fx/rates            汇率表 ?base=CNY -> {base, rates, sourceUpdatedAt, fetchedAt, stale, fromCache}（任意登录态；后台「汇率换算」试算工具用；数据源 open.er-api.com 免费无 key，按 base 落库缓存 12 小时，拉取失败回退旧缓存并标 stale，无缓存又失败才 502；存量库需跑 sql/fx-rates.sql）
GET  /api/fx/rates                  公开汇率表 ?base=CNY（前台币种切换用；与 admin 版共用 OverseasFxService 同一取数+缓存，返回形状一致，无需登录；站点配置另有 baseCurrency 字段=商品价格录入币种，默认 CNY，键值表存法无需跑 SQL）
GET|POST /api/admin/users           后台用户列表 / 新建（需 users 权限）
PUT|DELETE /api/admin/users/{id}    编辑（可重置密码）/ 删除（最后一个超级管理员受保护）
GET|POST /api/admin/roles           角色列表 / 新建（需 users 权限）
PUT|DELETE /api/admin/roles/{id}    编辑 / 删除（内置角色、仍有用户的角色不可删）
GET  /api/admin/permissions         菜单权限定义（角色编辑用）
```

**后台管理界面**：启动后打开 **`/admin`**（静态页在 `src/main/resources/static/admin/`，
与 Node 版共用同一套界面和接口契约），用账号密码登录（初始账号 admin / ADMIN_TOKEN），
页签按角色权限显示：商品管理、分类管理、询价列表、站点配置、用户与角色。
上传的图片存放在 `overseas.upload-dir`
（默认 `./data/uploads`，可用环境变量 `UPLOAD_DIR` 覆盖），经 `/uploads/**` 访问。

启动后 Swagger 文档在 `/swagger-ui.html`。


## 环境划分（与 tjmall 一致）

tjmall 的 yml 分 **4 个环境**，海外版照分：

| 环境 | tjmall（tjMall-app） | 海外版 | MySQL |
| --- | --- | --- | --- |
| dev | `application-dev.yml`，库 `merchant_single_open`（`${infra.host}:3306`） | `application-dev.yml` | `taojie_overseas_dev` |
| test | `application-test.yml`，独立测试库 `single_open_test` | `application-test.yml` | `taojie_overseas_test` |
| beta | `application-beta.yml`，与 dev 同构（主配置默认就跑 beta） | `application-beta.yml`（默认 active） | `taojie_overseas_beta` |
| prod | `application-prod.yml`，正式库、端口与池参数单独放大（initial/min-idle 10、max-wait 10000） | `application-prod.yml` | `taojie_overseas`（正式库，同 schema.sql） |

MySQL 配置方式完全按 tjmall：连接池 Druid、JDBC 地址用 `${infra.host}` 拼、
换环境只改 `infra.host` 与 profile。**唯一没照抄的**：tjmall 的 yml 里数据库密码是
明文写死的，海外仓库是 Public，所以密码一律改走环境变量 `DB_PASSWORD`（用户名
`DB_USER` 同理），yml 里不落明文。切换环境：改主配置 `spring.profiles.active`，
或启动加 `--spring.profiles.active=prod`。

各环境的库要先建好：dev/test/beta 把 `sql/schema.sql` 里的库名换成对应库名执行一次，
再导入 `sql/seed.sql` 即可（seed 里的 `USE taojie_overseas;` 一并改成目标库名）。

## 本地运行

要求：JDK 17、Maven 3.9+、MySQL 8。

```bash
# 1. 建库建表 + 演示数据（与 Node 版相同的 14 个示例商品）
mysql -u root -p < sql/schema.sql
mysql -u root -p taojie_overseas < sql/seed.sql

# 2. 启动（环境变量给数据库与管理口令）
DB_PASSWORD=你的密码 ADMIN_TOKEN=你的口令 mvn spring-boot:run   # 默认 beta 环境，连 taojie_overseas_beta
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
