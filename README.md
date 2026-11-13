# taojie-overseas-server

Lightweight backend for the Taojie **overseas PC mall** (`taojie-pc-overseas`).
It exists because the domestic `tjmall` front API is the mini-program's
login-based API (anonymous calls return 401) and has no English product
names, USD prices, MOQ or RFQ capture — so the overseas site gets its own
small backend, deployed next to the storefront (Jakarta), while product
data is imported/synced from tjmall rather than fetched at runtime.

**Zero npm dependencies** — Node ≥ 22 only (`node:http` + `node:sqlite`).
Data lives in one SQLite file (`data/app.db`), created and seeded on first
boot from `data/seed.json`.

## Run

```bash
cp .env.example .env   # set ADMIN_TOKEN at least
npm start              # http://localhost:3100
```

## API

Public (no login — the overseas showcase is for anonymous visitors):

| Endpoint | Description |
|---|---|
| `GET /api/health` | Liveness + product count |
| `GET /api/categories?lang=en` | Categories with per-category product counts (`lang`: en/zh/es/fr/pt) |
| `GET /api/products?lang=&keyword=&categoryId=&brandId=&page=1&limit=48` | Product list / search (filters stack) |
| `GET /api/products/:id?lang=` | Product detail |
| `GET /api/keywords` | Search hot words |
| `GET /api/site-config` | Storefront config (theme, announcement, contacts, baseCurrency, …) |
| `GET /api/fx/rates?base=CNY` | Public FX rates for the storefront currency switcher — same fetch + 12h cache as the admin calculator below, no login required |
| `GET /api/brands?lang=` | Active brands (products reference them by `brandId`) |
| `POST /api/rfq` | Submit an inquiry `{name, contact, product, qty, message}` — stored as a lead (rate-limited per IP) |
| `POST /api/chat/start` | Start a visitor chat session `{name?, token?, referrer?, page?, pageTitle?}` → `{sessionId, token}` (a known token reuses its session; IP geo + UA profile captured, geo best-effort) |
| `POST /api/chat/messages` | Visitor sends a chat message `{token, body}` (rate-limited per IP; re-opens a closed session) |
| `GET /api/chat/messages?token=&afterId=` | Visitor pulls own chat messages (incremental via `afterId`; marks agent messages read, returns `agentTyping`) |
| `POST /api/chat/ping` | Visitor heartbeat `{token, page?, pageTitle?}` — online presence + current page |
| `POST /api/chat/typing` | Visitor "typing…" signal `{token}` |

Admin auth: log in with a username + password — `POST /api/admin/login`
returns a session token that is then sent as the `x-admin-token` header.
Each account has a role; roles carry **menu permissions** (`products` /
`categories` / `rfq` / `chat` / `config` / `users`) which the API enforces per area
(403 when missing) and the UI uses to show/hide tabs. The `ADMIN_TOKEN`
itself still works as a break-glass root credential with all permissions.
First boot seeds three roles （超级管理员 / 运营 / 外贸员） and, while the user
table is empty, an initial account **admin** whose password is the
`ADMIN_TOKEN` value — log in with it, then change the password and add
people under 用户与角色.

| Endpoint | Description |
|---|---|
| `POST /api/admin/login` / `POST /api/admin/logout` / `GET /api/admin/me` | Login (public), logout, current account + permissions |
| `GET /api/admin/rfq` (perm `rfq`) | Latest 200 RFQ leads |
| `GET /api/admin/chat/sessions?keyword=` (perm `chat`) | Chat sessions by latest activity: last-message preview, unread count, visitor profile (geo/device/UA), online + bot-suspect flags; `keyword` matches visitor name or message text |
| `GET\|POST /api/admin/chat/sessions/:id/messages` (perm `chat`) | Full thread (reading marks visitor messages as read; session carries `visitorReadMsgId` + `visitorTyping`) / reply as agent |
| `POST /api/admin/chat/sessions/:id/typing` (perm `chat`) | Agent "typing…" signal |
| `PUT /api/admin/chat/sessions/:id/note` (perm `chat`) | Internal note on the session (never sent to the visitor) |
| `PUT /api/admin/chat/sessions/:id/status` (perm `chat`) | Close / re-open the session (`{status: open\|closed}`) |
| `PUT /api/admin/site-config` (perm `config`) | Update site config (only known fields are accepted) |
| `GET\|POST /api/admin/products`, `PUT\|DELETE /api/admin/products/:id` (perm `products`) | Product management (multi-language names, USD price, MOQ, specs, on/off shelf) |
| `GET\|POST /api/admin/categories`, `PUT\|DELETE /api/admin/categories/:id` (perm `categories`) | Category management (delete refuses while products use it) |
| `GET\|POST /api/admin/brands`, `PUT\|DELETE /api/admin/brands/:id` (perm `products`) | Brand management, mirroring plat's brand module (multi-language names, logo; delete refuses while products use it; saving a product with an unknown `brandId` is rejected) |
| `POST /api/admin/upload` (perm `products`) | Image/video upload as `{filename, dataBase64}` → `{url}` (images png/jpg/webp/gif ≤ 6 MB; videos mp4/webm/mov ≤ 50 MB; files land in `data/uploads/`, served at `/uploads/`) |
| `GET\|POST /api/admin/users`, `PUT\|DELETE /api/admin/users/:id` (perm `users`) | Admin accounts (create, assign role, enable/disable, reset password; last super admin is protected) |
| `GET\|POST /api/admin/roles`, `PUT\|DELETE /api/admin/roles/:id`, `GET /api/admin/permissions` (perm `users`) | Roles = menu-permission sets (built-in super role is locked to all permissions) |
| `GET /api/admin/fx/rates?base=CNY` (any admin account) | FX rates for the admin price calculator: `{base, rates, sourceUpdatedAt, fetchedAt, stale, fromCache}`. Source is open.er-api.com (free, no key, ~160 currencies, daily); cached per base in the `fx_rates` table for 12h; a failed refresh serves the stale cache flagged `stale`, and only no-cache + fetch-failed is a 502 |

## Admin UI （后台管理界面）

The management UI lives in the separate **`taojie-overseas-admin`** repository.
Run or deploy that Vue application and configure its `/api/` proxy to this backend
(default port 3100). This backend exposes `/api/admin/**` and `/uploads/**`;
it no longer serves the legacy `/admin` static page.

New RFQs are also POSTed as JSON to `RFQ_WEBHOOK_URL` when set (mail /
WhatsApp notification gateway hook).

## Name auto-translate (admin data entry)

The admin product/category/brand forms have an **自动翻译** button:
fill in only the Chinese name, and `POST /api/admin/translate`
(`{ text, targets? }` → `{ translations, errors }`, any admin login)
fills the other languages once, at entry time — the results are stored
in the `names` JSON like hand-typed ones, and the storefront is
unchanged. Supported targets (source is always zh): en, es, fr, pt, id,
ms, vi, th, ar, ru, de — the same list as the admin language registry
(`src/utils/langs.js` in taojie-overseas-admin); codes outside it come
back as `errors[code] = "unsupported"` and are never fetched.
Provider env vars:

- **default: self-hosted LibreTranslate** — `LIBRETRANSLATE_URL`
  (default `http://127.0.0.1:5000`, optional `LIBRETRANSLATE_API_KEY`
  when the instance requires one). `docker compose up -d` in this repo
  starts it (service `libretranslate`, bound to localhost only, models
  limited to the twelve languages via `LT_LOAD_ONLY`); the backend
  reaches it over the loopback/internal network, it is never exposed
  publicly.
- opt-in fallback: `TRANSLATE_PROVIDER=mymemory` uses the free
  **MyMemory** API instead; set `TRANSLATE_EMAIL` to raise its daily
  quota.
- per-request override: the request body may carry
  `provider: "mymemory" | "libre"` to pin the provider for that call
  only (any other value is a 400); the response echoes the provider
  actually used as `provider`, and MyMemory calls may add a `meta`
  object (`responseStatus`, and `quotaFinished` when the source
  response carried it — MyMemory offers no remaining-quota API, so no
  quota numbers are ever reported). The admin batch translate uses
  this to pin MyMemory without changing the server default.

There is no silent provider fallback: a language the self-hosted
service does not support is reported in `errors` (and left for manual
entry) exactly like any other failure; the endpoint only returns 502
when every target failed.

## Importing real products (from tjmall)

```bash
node scripts/import-products.mjs export.json
```

`export.json` uses the same shape as `data/seed.json`: categories with
per-language `names`, products with `names` per language, USD `price`,
`moq`, `specs`, etc. Imports upsert by product id, so the script can be
re-run for refreshes. The bundled seed contains the storefront's sample
catalogue so the stack runs end-to-end before the first real export.

## Deployment sketch (Jakarta server)

1. Install Node ≥ 22, copy this project, set `.env` (`PORT`, `ADMIN_TOKEN`).
2. Run under systemd or pm2 (`npm start`).
3. Nginx: serve the built frontend statically and reverse-proxy `/api/`
   to `127.0.0.1:3100`; put the CDN in front as usual (do not cache `/api/`).
4. Back up `data/app.db` daily — it holds the RFQ leads.
