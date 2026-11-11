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
| `GET /api/products?lang=&keyword=&categoryId=&page=1&limit=48` | Product list / search |
| `GET /api/products/:id?lang=` | Product detail |
| `GET /api/keywords` | Search hot words |
| `GET /api/site-config` | Storefront config (theme, announcement, contacts, …) |
| `POST /api/rfq` | Submit an inquiry `{name, contact, product, qty, message}` — stored as a lead (rate-limited per IP) |

Admin auth: log in with a username + password — `POST /api/admin/login`
returns a session token that is then sent as the `x-admin-token` header.
Each account has a role; roles carry **menu permissions** (`products` /
`categories` / `rfq` / `config` / `users`) which the API enforces per area
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
| `PUT /api/admin/site-config` (perm `config`) | Update site config (only known fields are accepted) |
| `GET\|POST /api/admin/products`, `PUT\|DELETE /api/admin/products/:id` (perm `products`) | Product management (multi-language names, USD price, MOQ, specs, on/off shelf) |
| `GET\|POST /api/admin/categories`, `PUT\|DELETE /api/admin/categories/:id` (perm `categories`) | Category management (delete refuses while products use it) |
| `POST /api/admin/upload` (perm `products`) | Image upload as `{filename, dataBase64}` → `{url}` (png/jpg/webp/gif, ≤ 6 MB; files land in `data/uploads/`, served at `/uploads/`) |
| `GET\|POST /api/admin/users`, `PUT\|DELETE /api/admin/users/:id` (perm `users`) | Admin accounts (create, assign role, enable/disable, reset password; last super admin is protected) |
| `GET\|POST /api/admin/roles`, `PUT\|DELETE /api/admin/roles/:id`, `GET /api/admin/permissions` (perm `users`) | Roles = menu-permission sets (built-in super role is locked to all permissions) |

## Admin UI （后台管理界面）

Open **`http://localhost:3100/admin`** — a small built-in management page
(static files in `admin/`, no build step). Log in with a username and
password (initial account: **admin** / the `ADMIN_TOKEN` value); the
session token is kept in sessionStorage and sent as `x-admin-token`.
Tabs follow the account's role: 商品管理 (products), 分类管理 (categories),
询价列表 (RFQs), 站点配置 (site config), 用户与角色 (users & roles) — a tab
the role does not include is not shown at all. The **Java implementation
serves the same UI and the same admin API**, so day-to-day management
looks identical whichever backend is deployed.

New RFQs are also POSTed as JSON to `RFQ_WEBHOOK_URL` when set (mail /
WhatsApp notification gateway hook).

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
