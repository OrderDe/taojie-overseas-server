// Taojie overseas mall — lightweight backend.
// Zero npm dependencies: Node >= 22 (uses node:http + node:sqlite).
//
// Serves the public catalogue API the overseas PC showcase expects:
//   GET  /api/health
//   GET  /api/categories?lang=en
//   GET  /api/products?lang=&keyword=&categoryId=&brandId=&page=1&limit=48
//   GET  /api/products/:id?lang=
//   GET  /api/keywords
//   GET  /api/site-config
//   POST /api/rfq                      (lead capture, stored in SQLite)
//   POST /api/chat/start               (visitor chat: create/reuse a session)
//   GET|POST /api/chat/messages        (visitor chat: pull (afterId) / send)
//   POST /api/chat/ping                (visitor heartbeat: online + current page)
//   POST /api/chat/typing              (visitor is typing)
//   GET  /api/formula/brands           (paint formula library: enabled car brands)
//   GET  /api/formula/models?brandId=  (car models, linked to the brand picker)
//   GET  /api/formula/search?brandId=&modelId=&keyword=&page=&pageSize=  (car colours)
//   GET  /api/formula/colors/:id       (colour detail: colour numbers + LAB + formula lines)
//   GET|POST|PUT|DELETE /api/admin/formula/<entity>[/:id]  (formula dict CRUD; perm formula)
//   GET|POST|PUT|DELETE /api/admin/formula-numbers[/:id]   (colour numbers CRUD; perm formula)
//   GET|PUT /api/admin/formula-numbers/:id/lines           (replace a number's formula lines)
//   POST /api/track                    (visitor statistics beacon: view/click/heartbeat)
//   GET  /api/admin/visitors           (visitor stats: summary/countries/trend/list; perm visitors)
//   GET  /api/admin/visitors/:token/events  (one visitor's view+click timeline)
//   GET  /api/admin/chat/sessions?keyword=  (chat sessions + unread + profile)
//   GET|POST /api/admin/chat/sessions/:id/messages  (read thread / reply)
//   POST /api/admin/chat/sessions/:id/typing        (agent is typing)
//   PUT  /api/admin/chat/sessions/:id/note          (internal note, agents only)
//   PUT  /api/admin/chat/sessions/:id/status        (open/closed)
//   GET  /api/admin/rfq                (header: x-admin-token)
//   PUT  /api/admin/site-config        (header: x-admin-token)
//   POST /api/admin/login              (username+password -> session token)
//   GET|POST|PUT|DELETE /api/admin/products[/:id]     (catalogue management)
//   GET|POST|PUT|DELETE /api/admin/categories[/:id]   (category management)
//   GET|POST|PUT|DELETE /api/admin/brands[/:id]       (brand management, plat-style)
//   GET|POST|PUT|DELETE /api/admin/users[/:id]        (admin accounts)
//   GET|POST|PUT|DELETE /api/admin/roles[/:id]        (roles = menu permissions)
//   POST /api/admin/upload             (JSON {filename, dataBase64} -> {url}; images <= 6 MB, videos mp4/webm/mov <= 50 MB)
//   POST /api/admin/translate          (zh name -> 11 target languages, entry-time only)
//   GET  /api/admin/fx/rates?base=CNY  (FX rates for the price calculator; any admin account, 12h cache)
//   GET  /api/fx/rates?base=CNY        (public twin of the above — storefront currency switcher, same cache)
//   GET  /admin                        (admin UI, static files in admin/)
//   GET  /uploads/<file>               (uploaded product images / videos)
//
// Data lives in a single SQLite file (data/app.db). On first boot the
// bundled seed (data/seed.json) is imported; replace/extend it with real
// products via scripts/import-products.mjs (tjmall export -> upsert).

import { createServer } from 'node:http'
import { DatabaseSync } from 'node:sqlite'
import { scryptSync, randomBytes, timingSafeEqual } from 'node:crypto'
import { readFileSync, existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join, extname, basename } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const PORT = Number(process.env.PORT || 3100)
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || ''
const DB_PATH = process.env.DB_PATH || join(ROOT, 'data', 'app.db')
const ADMIN_DIR = join(ROOT, 'admin')
const UPLOAD_DIR = process.env.UPLOAD_DIR || join(ROOT, 'data', 'uploads')
const LANGS = ['en', 'zh', 'es', 'fr', 'pt']

mkdirSync(dirname(DB_PATH), { recursive: true })
const db = new DatabaseSync(DB_PATH)
db.exec(`
  CREATE TABLE IF NOT EXISTS categories (
    id TEXT PRIMARY KEY,
    names TEXT NOT NULL DEFAULT '{}',
    sort INTEGER NOT NULL DEFAULT 0
  );
  CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY,
    source_id TEXT NOT NULL DEFAULT '',
    category_id TEXT NOT NULL DEFAULT '',
    names TEXT NOT NULL DEFAULT '{}',
    price REAL NOT NULL DEFAULT 0,
    ot_price REAL NOT NULL DEFAULT 0,
    sales INTEGER NOT NULL DEFAULT 0,
    unit TEXT NOT NULL DEFAULT 'pc',
    moq INTEGER NOT NULL DEFAULT 0,
    image TEXT NOT NULL DEFAULT '',
    video TEXT NOT NULL DEFAULT '',
    tag TEXT NOT NULL DEFAULT '',
    blurb TEXT NOT NULL DEFAULT '',
    specs TEXT NOT NULL DEFAULT '{}',
    active INTEGER NOT NULL DEFAULT 1
  );
  CREATE TABLE IF NOT EXISTS rfq (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    contact TEXT NOT NULL,
    product TEXT NOT NULL DEFAULT '',
    qty TEXT NOT NULL DEFAULT '',
    message TEXT NOT NULL DEFAULT '',
    lang TEXT NOT NULL DEFAULT 'en',
    ip TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL
  );
  CREATE TABLE IF NOT EXISTS site_config (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    data TEXT NOT NULL DEFAULT '{}'
  );
  CREATE TABLE IF NOT EXISTS fx_rates (
    base TEXT PRIMARY KEY,
    rates TEXT NOT NULL DEFAULT '{}',
    source_updated_at TEXT NOT NULL DEFAULT '',
    fetched_at TEXT NOT NULL DEFAULT ''
  );
  CREATE TABLE IF NOT EXISTS brands (
    id TEXT PRIMARY KEY,
    names TEXT NOT NULL DEFAULT '{}',
    logo TEXT NOT NULL DEFAULT '',
    sort INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1
  );
  CREATE TABLE IF NOT EXISTS admin_roles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    permissions TEXT NOT NULL DEFAULT '[]',
    builtin INTEGER NOT NULL DEFAULT 0
  );
  CREATE TABLE IF NOT EXISTS admin_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL DEFAULT '',
    display_name TEXT NOT NULL DEFAULT '',
    role_id INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT ''
  );
  CREATE TABLE IF NOT EXISTS admin_sessions (
    token TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    expires_at INTEGER NOT NULL
  );
  CREATE TABLE IF NOT EXISTS chat_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    visitor_token TEXT NOT NULL UNIQUE,
    visitor_name TEXT NOT NULL DEFAULT '访客',
    lang TEXT NOT NULL DEFAULT 'en',
    created_at TEXT NOT NULL,
    last_msg_at TEXT NOT NULL DEFAULT '',
    last_agent_read_msg_id INTEGER NOT NULL DEFAULT 0,
    ip TEXT NOT NULL DEFAULT '',
    country TEXT NOT NULL DEFAULT '',
    region TEXT NOT NULL DEFAULT '',
    city TEXT NOT NULL DEFAULT '',
    timezone TEXT NOT NULL DEFAULT '',
    device TEXT NOT NULL DEFAULT '',
    browser TEXT NOT NULL DEFAULT '',
    os TEXT NOT NULL DEFAULT '',
    referrer TEXT NOT NULL DEFAULT '',
    current_page TEXT NOT NULL DEFAULT '',
    page_views INTEGER NOT NULL DEFAULT 0,
    last_seen_at TEXT NOT NULL DEFAULT '',
    suspect_bot INTEGER NOT NULL DEFAULT 0,
    visitor_typing_at TEXT NOT NULL DEFAULT '',
    agent_typing_at TEXT NOT NULL DEFAULT '',
    visitor_read_msg_id INTEGER NOT NULL DEFAULT 0,
    note TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'open'
  );
  CREATE TABLE IF NOT EXISTS chat_messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    sender TEXT NOT NULL,
    body TEXT NOT NULL,
    created_at TEXT NOT NULL
  );
  CREATE TABLE IF NOT EXISTS visitors (
    token TEXT NOT NULL UNIQUE,
    ip TEXT NOT NULL DEFAULT '',
    country TEXT NOT NULL DEFAULT '',
    region TEXT NOT NULL DEFAULT '',
    city TEXT NOT NULL DEFAULT '',
    timezone TEXT NOT NULL DEFAULT '',
    device TEXT NOT NULL DEFAULT '',
    browser TEXT NOT NULL DEFAULT '',
    os TEXT NOT NULL DEFAULT '',
    is_bot INTEGER NOT NULL DEFAULT 0,
    referrer TEXT NOT NULL DEFAULT '',
    first_page TEXT NOT NULL DEFAULT '',
    current_page TEXT NOT NULL DEFAULT '',
    page_views INTEGER NOT NULL DEFAULT 0,
    visits INTEGER NOT NULL DEFAULT 0,
    first_seen TEXT NOT NULL DEFAULT '',
    last_seen TEXT NOT NULL DEFAULT '',
    visit_started_at TEXT NOT NULL DEFAULT '',
    dwell_sec INTEGER NOT NULL DEFAULT 0
  );
  CREATE TABLE IF NOT EXISTS visit_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    token TEXT NOT NULL DEFAULT '',
    kind TEXT NOT NULL DEFAULT 'view',
    page TEXT NOT NULL DEFAULT '',
    action TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL DEFAULT ''
  );
`)
// Products gained a sort column after the first release; add it in place.
try { db.exec('ALTER TABLE products ADD COLUMN sort INTEGER NOT NULL DEFAULT 0') } catch { /* already there */ }
// Products gained a brand link when brand management landed (plat has
// brands as a first-class catalogue entity, products reference one).
try { db.exec("ALTER TABLE products ADD COLUMN brand_id TEXT NOT NULL DEFAULT ''") } catch { /* already there */ }
// Products gained one optional demo video (URL, '' = none) alongside the
// single image when the admin console grew a video upload.
try { db.exec("ALTER TABLE products ADD COLUMN video TEXT NOT NULL DEFAULT ''") } catch { /* already there */ }
// Chat sessions gained a visitor profile (geo/device/UA), presence
// (last_seen), typing + read watermarks, an internal note and a lifecycle
// status when the chat grew into a small support console. Add in place.
for (const col of [
  "ip TEXT NOT NULL DEFAULT ''", "country TEXT NOT NULL DEFAULT ''",
  "region TEXT NOT NULL DEFAULT ''", "city TEXT NOT NULL DEFAULT ''",
  "timezone TEXT NOT NULL DEFAULT ''", "device TEXT NOT NULL DEFAULT ''",
  "browser TEXT NOT NULL DEFAULT ''", "os TEXT NOT NULL DEFAULT ''",
  "referrer TEXT NOT NULL DEFAULT ''", "current_page TEXT NOT NULL DEFAULT ''",
  "page_views INTEGER NOT NULL DEFAULT 0", "last_seen_at TEXT NOT NULL DEFAULT ''",
  "suspect_bot INTEGER NOT NULL DEFAULT 0", "visitor_typing_at TEXT NOT NULL DEFAULT ''",
  "agent_typing_at TEXT NOT NULL DEFAULT ''", "visitor_read_msg_id INTEGER NOT NULL DEFAULT 0",
  "note TEXT NOT NULL DEFAULT ''", "status TEXT NOT NULL DEFAULT 'open'"
]) {
  try { db.exec(`ALTER TABLE chat_sessions ADD COLUMN ${col}`) } catch { /* already there */ }
}
// ---- Paint formula library (replicated from the sfsy/JBolt system) ----
// Same table names, column names and column order as the source MySQL
// schema (sfsy_db_new), with types loosely mapped to SQLite (bigint/int
// -> INTEGER, varchar/char -> TEXT, double -> REAL, datetime -> TEXT).
// Data arrives via tools/import-sfsy.js from the bundled dump
// (data/sfsy_data.sql.gz). The bh/_0 tables are the source system's
// numbered/backup variants — replicated as-is, not yet queried.
// Keep this DDL in sync with tools/import-sfsy.js.
db.exec(`
  CREATE TABLE IF NOT EXISTS car_brand (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT, name_first TEXT, country TEXT, url TEXT,
    letter_rank INTEGER, enable TEXT DEFAULT '1', is_deleted TEXT DEFAULT '0',
    create_time TEXT, update_time TEXT, create_user_id TEXT, update_user_id TEXT
  );
  CREATE TABLE IF NOT EXISTS car_model (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    brand TEXT, brand_id INTEGER, model TEXT NOT NULL, sort_rank INTEGER,
    enable TEXT DEFAULT '1', update_time TEXT, is_deleted TEXT DEFAULT '0',
    create_time TEXT, create_user_id TEXT, update_user_id TEXT
  );
  CREATE TABLE IF NOT EXISTS car_country (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT, name_en TEXT, language TEXT, code INTEGER, state INTEGER,
    enable TEXT, sort_rank INTEGER
  );
  CREATE TABLE IF NOT EXISTS car_factory (
    id INTEGER PRIMARY KEY,
    name TEXT, brand_id INTEGER, country TEXT, state INTEGER,
    enable TEXT, sort_rank INTEGER
  );
  CREATE TABLE IF NOT EXISTS color_card (
    id INTEGER PRIMARY KEY,
    name TEXT, code TEXT, enable TEXT, is_deleted TEXT DEFAULT '0',
    create_time TEXT, update_time TEXT, create_user_id TEXT, update_user_id TEXT
  );
  CREATE TABLE IF NOT EXISTS color_masterbatch (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    model TEXT NOT NULL UNIQUE, name TEXT NOT NULL, eng_name TEXT NOT NULL,
    type TEXT, mark TEXT, enable TEXT, is_deleted TEXT DEFAULT '0',
    create_time TEXT, update_time TEXT, create_user_id TEXT, update_user_id TEXT
  );
  CREATE TABLE IF NOT EXISTS car_color (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    brand_id INTEGER NOT NULL, model_id INTEGER NOT NULL,
    car_color TEXT, factory_color TEXT, color_num TEXT, card_num TEXT,
    car_image TEXT DEFAULT 'https://cdn.tujia.club/tujia/car/car.jpg',
    enable TEXT DEFAULT '1', is_deleted TEXT DEFAULT '0',
    create_time TEXT, update_time TEXT, create_user_id TEXT, update_user_id TEXT
  );
  CREATE TABLE IF NOT EXISTS car_color0 (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    brand_id INTEGER NOT NULL, model_id INTEGER NOT NULL,
    car_color TEXT, factory_color TEXT, color_num TEXT, card_num TEXT,
    car_image TEXT DEFAULT 'https://cdn.tujia.club/tujia/car/car.jpg',
    enable TEXT DEFAULT '1', is_deleted TEXT DEFAULT '0',
    create_time TEXT, update_time TEXT, create_user_id TEXT, update_user_id TEXT
  );
  CREATE TABLE IF NOT EXISTS car_color_bh (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    brand_id INTEGER, model_id INTEGER, car_color TEXT, factory_color TEXT,
    innercode TEXT, description TEXT, car_image TEXT, enable TEXT DEFAULT '1',
    year TEXT DEFAULT '0', devcount TEXT, devgroup TEXT, colorcode TEXT,
    innergroup TEXT, formulas TEXT, devsite TEXT
  );
  CREATE TABLE IF NOT EXISTS color_formula (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    colornum_id INTEGER NOT NULL, name TEXT, model TEXT NOT NULL,
    dose REAL NOT NULL, color_rank INTEGER NOT NULL,
    enable TEXT, is_deleted TEXT DEFAULT '0',
    create_time TEXT, update_time TEXT, create_user_id TEXT, update_user_id TEXT
  );
  CREATE TABLE IF NOT EXISTS color_formula_bh (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    innergroup TEXT NOT NULL, fname TEXT, fdesc TEXT, lalm REAL, ordeid INTEGER,
    enable TEXT, is_deleted TEXT DEFAULT '0',
    create_time TEXT, update_time TEXT, create_user_id TEXT, update_user_id TEXT
  );
  CREATE TABLE IF NOT EXISTS color_number (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL, color_name TEXT, lab_l REAL, lab_a REAL, lab_b REAL,
    enable TEXT DEFAULT '1', is_deleted TEXT DEFAULT '0',
    create_time TEXT, update_time TEXT, create_user_id TEXT, update_user_id TEXT
  );
  CREATE TABLE IF NOT EXISTS color_number_relation (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    car_color INTEGER, color_number INTEGER, enable TEXT DEFAULT '1'
  );
  CREATE TABLE IF NOT EXISTS color_brand (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT, logo TEXT, enable TEXT DEFAULT '1',
    update_time TEXT, create_time TEXT, create_user_id TEXT, update_user_id TEXT,
    is_deleted TEXT DEFAULT '0'
  );
  CREATE TABLE IF NOT EXISTS color_collection (
    id INTEGER PRIMARY KEY,
    wechat_user_id INTEGER NOT NULL, nickname TEXT, model_id INTEGER,
    color_id INTEGER, brand_id INTEGER, mp_id INTEGER, create_time TEXT
  );
  CREATE INDEX IF NOT EXISTS idx_car_model_brand ON car_model (brand_id);
  CREATE INDEX IF NOT EXISTS idx_car_color_brand_model ON car_color (brand_id, model_id);
  CREATE INDEX IF NOT EXISTS idx_color_formula_num ON color_formula (colornum_id);
  CREATE INDEX IF NOT EXISTS idx_relation_color ON color_number_relation (car_color);
  CREATE INDEX IF NOT EXISTS idx_relation_number ON color_number_relation (color_number);
`)
mkdirSync(UPLOAD_DIR, { recursive: true })

function seedIfEmpty() {
  const { n } = db.prepare('SELECT COUNT(*) AS n FROM products').get()
  if (n > 0) return
  const seedPath = join(ROOT, 'data', 'seed.json')
  if (!existsSync(seedPath)) return
  const seed = JSON.parse(readFileSync(seedPath, 'utf8'))
  const insC = db.prepare('INSERT OR REPLACE INTO categories (id, names, sort) VALUES (?, ?, ?)')
  ;(seed.categories || []).forEach((c, i) => insC.run(String(c.id), JSON.stringify(c.names || { en: c.name || '' }), i))
  const insP = db.prepare(`INSERT OR REPLACE INTO products
    (id, source_id, category_id, names, price, ot_price, sales, unit, moq, image, video, tag, blurb, specs, active)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)`)
  for (const p of seed.products || []) {
    insP.run(p.id, String(p.sourceId ?? p.id), String(p.categoryId || ''),
      JSON.stringify(p.names || { en: p.name || '' }), Number(p.price) || 0, Number(p.otPrice) || 0,
      Number(p.sales) || 0, String(p.unit || 'pc'), Number(p.moq) || 0, String(p.image || ''),
      String(p.video || ''), String(p.tag || ''), String(p.blurb || ''), JSON.stringify(p.specs || {}))
  }
  console.log(`seeded ${seed.products?.length || 0} products, ${seed.categories?.length || 0} categories`)
}
seedIfEmpty()

// Formula library first boot: when the formula tables are still empty
// and the repo carries the sfsy data file, import it automatically —
// the same code path as `node tools/import-sfsy.js`, so no manual step
// is needed after a fresh deploy. Tables that already hold rows are
// skipped; any failure is logged and never blocks startup.
try {
  const { n } = db.prepare('SELECT COUNT(*) AS n FROM car_brand').get()
  if (Number(n) === 0) {
    const tool = await import(pathToFileURL(join(ROOT, 'tools', 'import-sfsy.js')).href)
    const file = tool.findBundledDataFile(ROOT)
    if (file) {
      const results = tool.importIntoDb(db, tool.loadDumpRows(file))
      console.log(`sfsy formula data auto-imported from ${basename(file)}:`)
      for (const line of results) console.log(`  ${line}`)
    }
  }
} catch (e) {
  console.error('sfsy formula auto-import skipped:', e.message)
}

const DEFAULT_CONFIG = {
  defaultTheme: 'h', defaultLanguage: 'en', heroTitle: '', heroSubtitle: '',
  announcement: '', showRfq: true, showCart: false, showDealer: false, showContact: true, whatsapp: '', email: '', currency: 'USD',
  baseCurrency: 'CNY', // currency the stored product prices are in; the storefront converts for display
  logo: '', // header logo image URL; empty = storefront shows the text store name
  // Homepage content blocks (admin-editable; empty = storefront falls back
  // to its built-in theme/i18n defaults). navItems is
  // [{label, link, type?, categoryId?}] — type is 'category' | 'anchor' |
  // 'external'; older items without a type are inferred from link upstream.
  navItems: [], heroEyebrow: '', dealerTitle: '', dealerText: '',
  dealerButton: '', hotline: '', storyTitle: '', storyText: '',
  // Company profile page: when set, the storefront "profile" view embeds
  // this URL full-page in an iframe (mirrors the original Willis site,
  // whose profile page embeds the company homepage). Empty = the
  // storefront falls back to the storyTitle/storyText text version.
  profileUrl: 'https://williscarpaint.com/',
  // Social/contact jump links for the storefront "Start Chat with" panel.
  // Full profile URLs (empty = icon hidden); wechat is an ID (tap copies).
  // whatsapp / email / hotline above are reused by the same panel.
  tiktok: '', instagram: '', facebook: '', youtube: '', wechat: ''
}
function getConfig() {
  const row = db.prepare('SELECT data FROM site_config WHERE id = 1').get()
  if (!row) return { ...DEFAULT_CONFIG }
  try { return { ...DEFAULT_CONFIG, ...JSON.parse(row.data) } } catch { return { ...DEFAULT_CONFIG } }
}

function pickLang(namesJson, lang) {
  let names = {}
  try { names = JSON.parse(namesJson || '{}') } catch { /* keep empty */ }
  return names[lang] || names.en || Object.values(names)[0] || ''
}

function brandName(brandId, lang) {
  if (!brandId) return ''
  const brand = db.prepare('SELECT names FROM brands WHERE id = ?').get(brandId)
  return brand ? pickLang(brand.names, lang) : ''
}

function productOut(row, lang) {
  const cat = db.prepare('SELECT names FROM categories WHERE id = ?').get(row.category_id)
  let specs = {}
  try { specs = JSON.parse(row.specs || '{}') } catch { /* keep empty */ }
  return {
    id: row.id,
    name: pickLang(row.names, lang),
    categoryId: row.category_id,
    category: cat ? pickLang(cat.names, lang) : '',
    brandId: row.brand_id || '',
    brand: brandName(row.brand_id, lang),
    price: row.price,
    otPrice: row.ot_price,
    sales: row.sales,
    unit: row.unit,
    moq: row.moq,
    image: row.image,
    video: row.video || '',
    tag: row.tag,
    description: row.blurb,
    specs
  }
}

function send(res, status, body) {
  const text = JSON.stringify(body)
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, x-admin-token'
  })
  res.end(text)
}

function readBody(req, limit = 1e6) {
  return new Promise((resolve, reject) => {
    let data = ''
    req.on('data', (c) => { data += c; if (data.length > limit) req.destroy() })
    req.on('end', () => {
      if (!data) return resolve({})
      try { resolve(JSON.parse(data)) } catch { reject(new Error('invalid json')) }
    })
    req.on('error', reject)
  })
}

// ---- Admin accounts: users, roles (menu permissions), sessions ----
// The shared ADMIN_TOKEN still works as a break-glass root credential. On
// top of it there are real accounts: POST /api/admin/login checks a
// username+password and issues a session token; each role carries menu
// permissions (products / categories / rfq / config / users) which the API
// enforces per area and the admin UI uses to show/hide tabs.
const PERMISSIONS = [
  { key: 'products', label: '商品管理' },
  { key: 'categories', label: '分类管理' },
  { key: 'rfq', label: '询价列表' },
  { key: 'chat', label: '在线聊天' },
  { key: 'visitors', label: '访客统计' },
  { key: 'formula', label: '配方管理' },
  { key: 'config', label: '站点配置' },
  { key: 'users', label: '用户与角色' }
]
const ALL_PERMS = PERMISSIONS.map((p) => p.key)
const SESSION_TTL = 7 * 24 * 3600 * 1000

function hashPassword(password) {
  const salt = randomBytes(16)
  const hash = scryptSync(String(password), salt, 64)
  return `scrypt:${salt.toString('hex')}:${hash.toString('hex')}`
}
function verifyPassword(password, stored) {
  const parts = String(stored || '').split(':')
  if (parts.length !== 3 || parts[0] !== 'scrypt') return false
  const expected = Buffer.from(parts[2], 'hex')
  const actual = scryptSync(String(password), Buffer.from(parts[1], 'hex'), 64)
  return expected.length === actual.length && timingSafeEqual(expected, actual)
}

function seedAdminAccounts() {
  const { n } = db.prepare('SELECT COUNT(*) AS n FROM admin_roles').get()
  if (n === 0) {
    const ins = db.prepare('INSERT INTO admin_roles (name, permissions, builtin) VALUES (?, ?, ?)')
    ins.run('超级管理员', JSON.stringify(['*']), 1)
    ins.run('运营', JSON.stringify(['products', 'categories', 'rfq', 'chat', 'config', 'visitors', 'formula']), 0)
    ins.run('外贸员', JSON.stringify(['rfq', 'chat', 'visitors']), 0)
  }
  const { n: userCount } = db.prepare('SELECT COUNT(*) AS n FROM admin_users').get()
  if (userCount === 0 && ADMIN_TOKEN) {
    const role = db.prepare("SELECT id FROM admin_roles WHERE name = '超级管理员'").get()
    db.prepare('INSERT INTO admin_users (username, password_hash, display_name, role_id, active, created_at) VALUES (?, ?, ?, ?, 1, ?)')
      .run('admin', hashPassword(ADMIN_TOKEN), '管理员', role.id, new Date().toISOString())
    console.log('created initial admin account: admin (password = ADMIN_TOKEN)')
  }
}
seedAdminAccounts()

function hasPerm(perms, key) {
  return perms.includes('*') || perms.includes(key)
}
function isSuperRole(roleId) {
  const role = db.prepare('SELECT * FROM admin_roles WHERE id = ?').get(roleId)
  return role ? parseJsonCol(role.permissions, []).includes('*') : false
}
function otherSuperExists(excludeUserId) {
  const rows = db.prepare('SELECT id, role_id FROM admin_users WHERE active = 1 AND id != ?').all(excludeUserId)
  return rows.some((u) => isSuperRole(u.role_id))
}

// Resolve the caller from the x-admin-token header: the root ADMIN_TOKEN,
// or a login session token. Returns the principal or null.
function resolveAuth(req) {
  const token = req.headers['x-admin-token']
  if (!token) return null
  if (ADMIN_TOKEN !== '' && token === ADMIN_TOKEN) {
    return { userId: 0, username: 'root', displayName: '超级管理员（令牌）', roleName: '超级管理员', permissions: ['*'] }
  }
  const session = db.prepare('SELECT * FROM admin_sessions WHERE token = ?').get(token)
  if (!session || session.expires_at < Date.now()) return null
  const user = db.prepare('SELECT * FROM admin_users WHERE id = ?').get(session.user_id)
  if (!user || user.active !== 1) return null
  const role = db.prepare('SELECT * FROM admin_roles WHERE id = ?').get(user.role_id)
  return {
    userId: user.id, username: user.username, displayName: user.display_name,
    roleName: role ? role.name : '',
    permissions: role ? parseJsonCol(role.permissions, []) : []
  }
}

function isAdmin(req) {
  return resolveAuth(req) !== null
}

// Which permission a path needs (undefined = public login, null = any account).
function requiredPerm(path) {
  if (path === '/api/admin/login') return undefined
  if (path === '/api/admin/me' || path === '/api/admin/logout' || path === '/api/admin/fx/rates') return null
  if (path.startsWith('/api/admin/users') || path.startsWith('/api/admin/roles') || path === '/api/admin/permissions') return 'users'
  if (path.startsWith('/api/admin/products') || path.startsWith('/api/admin/brands') || path === '/api/admin/upload') return 'products'
  if (path.startsWith('/api/admin/categories')) return 'categories'
  if (path === '/api/admin/rfq') return 'rfq'
  if (path.startsWith('/api/admin/chat')) return 'chat'
  if (path.startsWith('/api/admin/visitors')) return 'visitors'
  if (path.startsWith('/api/admin/formula')) return 'formula'
  if (path === '/api/admin/site-config') return 'config'
  return null
}

function adminUserOut(u) {
  const role = db.prepare('SELECT name FROM admin_roles WHERE id = ?').get(u.role_id)
  return { id: u.id, username: u.username, displayName: u.display_name, roleId: u.role_id, roleName: role ? role.name : '', active: u.active === 1, createdAt: u.created_at }
}
function adminRoleOut(r) {
  const { n } = db.prepare('SELECT COUNT(*) AS n FROM admin_users WHERE role_id = ?').get(r.id)
  return { id: r.id, name: r.name, permissions: parseJsonCol(r.permissions, []), builtin: r.builtin === 1, userCount: n }
}

// Simple per-IP rate limit for POST /api/rfq: 5 per minute.
const rfqHits = new Map()
function rfqAllowed(ip) {
  const now = Date.now()
  const arr = (rfqHits.get(ip) || []).filter((t) => now - t < 60000)
  if (arr.length >= 5) return false
  arr.push(now)
  rfqHits.set(ip, arr)
  return true
}

// ---- Online chat ----
// A visitor is identified only by a random token the widget keeps in
// localStorage (no account). Agents read/reply from the admin chat page;
// a session tracks how far the agent has read so the list can show the
// count of unread visitor messages.
function hitAllowed(store, ip, maxPerMinute) {
  const now = Date.now()
  const arr = (store.get(ip) || []).filter((t) => now - t < 60000)
  if (arr.length >= maxPerMinute) return false
  arr.push(now)
  store.set(ip, arr)
  return true
}
const chatStartHits = new Map()
const chatMsgHits = new Map()

function chatSessionByToken(token) {
  if (!token) return undefined
  return db.prepare('SELECT * FROM chat_sessions WHERE visitor_token = ?').get(token)
}
function chatMessageOut(m) {
  return { id: m.id, sessionId: m.session_id, sender: m.sender, body: m.body, createdAt: m.created_at }
}
function insertChatMessage(sessionId, sender, body) {
  const nowIso = new Date().toISOString()
  const info = db.prepare('INSERT INTO chat_messages (session_id, sender, body, created_at) VALUES (?, ?, ?, ?)')
    .run(sessionId, sender, body, nowIso)
  // Sending a message is also a presence signal for the visitor.
  if (sender === 'visitor') {
    db.prepare('UPDATE chat_sessions SET last_msg_at = ?, last_seen_at = ? WHERE id = ?').run(nowIso, nowIso, sessionId)
  } else {
    db.prepare('UPDATE chat_sessions SET last_msg_at = ? WHERE id = ?').run(nowIso, sessionId)
  }
  return { id: Number(info.lastInsertRowid), sessionId, sender, body, createdAt: nowIso }
}

function clientIpOf(req) {
  return (req.headers['x-forwarded-for'] || req.socket.remoteAddress || '').toString().split(',')[0].trim()
}

// Tiny hand-rolled User-Agent reader (zero dependencies): device class,
// browser, OS and a bot suspicion flag. Bots cannot be proven either way —
// this is a signal for the agent, never a hard block.
function parseUserAgent(uaRaw) {
  const ua = String(uaRaw || '')
  const low = ua.toLowerCase()
  const suspectBot = !ua || /bot|crawler|spider|headless|curl|wget|python-requests|scrapy|httpclient|okhttp|java\/|go-http|postman|insomnia/.test(low)
  let device = 'desktop'
  if (/ipad|tablet|playbook|silk/.test(low) || (/android/.test(low) && !/mobile/.test(low))) device = 'tablet'
  else if (/mobile|iphone|ipod|android|windows phone|blackberry/.test(low)) device = 'mobile'
  let browser = ''
  if (/edg\//.test(low)) browser = 'Edge'
  else if (/opr\/|opera/.test(low)) browser = 'Opera'
  else if (/chrome\//.test(low)) browser = 'Chrome'
  else if (/firefox\//.test(low)) browser = 'Firefox'
  else if (/safari\//.test(low)) browser = 'Safari'
  else if (/msie|trident/.test(low)) browser = 'IE'
  let os = ''
  if (/windows/.test(low)) os = 'Windows'
  else if (/android/.test(low)) os = 'Android'
  else if (/iphone|ipad|ipod/.test(low)) os = 'iOS'
  else if (/mac os|macintosh/.test(low)) os = 'macOS'
  else if (/linux/.test(low)) os = 'Linux'
  return { device, browser, os, suspectBot }
}

function isPrivateIp(ip) {
  if (!ip) return true
  if (ip === '::1' || ip.startsWith('127.') || ip.startsWith('10.') || ip.startsWith('192.168.')) return true
  const m = ip.match(/^172\.(\d+)\./)
  return !!m && Number(m[1]) >= 16 && Number(m[1]) <= 31
}

// Best-effort geo lookup for a public IP via ip-api.com (free, no key).
// Called fire-and-forget after the session row exists: a slow or failed
// lookup leaves the geo fields empty and never delays session creation.
function lookupChatGeo(sessionId, ip) {
  const ctrl = new AbortController()
  const t = setTimeout(() => ctrl.abort(), 2500)
  fetch(`http://ip-api.com/json/${encodeURIComponent(ip)}?fields=status,country,regionName,city,timezone`, { signal: ctrl.signal })
    .then((r) => r.json())
    .then((g) => {
      if (g && g.status === 'success') {
        db.prepare('UPDATE chat_sessions SET country = ?, region = ?, city = ?, timezone = ? WHERE id = ?')
          .run(String(g.country || ''), String(g.regionName || ''), String(g.city || ''), String(g.timezone || ''), sessionId)
      }
    })
    .catch(() => { /* geo is best-effort only */ })
    .finally(() => clearTimeout(t))
}

// ---- Visitor statistics (site-wide tracking, separate from chat) ----
// The storefront beacon (utils/track.js) reports every visitor, not only
// the ones who open the chat widget. One row per visitor token in
// `visitors`, plus one row per view/click in `visit_events` (PV/UV and
// the 7-day trend are computed from view events only — clicks never
// count as page views).
//
// Definitions (also what the admin page shows):
// - a "visit" is a run of activity with no gap over 30 minutes; a longer
//   gap starts a new visit (visits + 1, visit_started_at reset).
// - dwell time accrues on every view/heartbeat/click by the gap since
//   last_seen, capped at 90s per step so an idle or backgrounded tab
//   cannot inflate it; the first sighting and a new visit add nothing.
// - "online" = last_seen within 60 seconds.
// - "today" = the server's local calendar day (boundaries computed in
//   local time below, stored timestamps are ISO/UTC strings).
const VISIT_GAP_MS = 30 * 60 * 1000
const DWELL_CAP_SEC = 90
const ONLINE_MS = 60 * 1000
const trackHits = new Map()
const TRACK_TOKEN_RE = /^[A-Za-z0-9_-]{8,128}$/

function visitorByToken(token) {
  if (!token) return undefined
  return db.prepare('SELECT * FROM visitors WHERE token = ?').get(token)
}

// Apply the dwell/visit bookkeeping for an activity stamp on an existing
// visitor row. Returns the fields to persist.
function accrueVisit(row, nowMs) {
  const lastMs = Date.parse(row.last_seen || '') || 0
  let { visits, dwell_sec: dwellSec } = row
  let visitStarted = row.visit_started_at || row.first_seen || new Date(nowMs).toISOString()
  if (!lastMs || nowMs - lastMs > VISIT_GAP_MS) {
    visits = (visits || 0) + 1
    visitStarted = new Date(nowMs).toISOString()
  } else {
    dwellSec = (dwellSec || 0) + Math.min(DWELL_CAP_SEC, Math.max(0, Math.floor((nowMs - lastMs) / 1000)))
  }
  return { visits, dwellSec, visitStarted }
}

function visitorOut(v) {
  const now = Date.now()
  const lastMs = Date.parse(v.last_seen || '') || 0
  const online = !!lastMs && now - lastMs < ONLINE_MS
  const startedMs = Date.parse(v.visit_started_at || '') || lastMs
  // Current-visit dwell: live visitors count up to "now" when the admin
  // page refreshes; offline visitors stop at their last_seen.
  const currentDwellSec = Math.max(0, Math.floor(((online ? now : lastMs) - startedMs) / 1000))
  return {
    token: v.token, ip: v.ip || '', country: v.country || '', region: v.region || '',
    city: v.city || '', timezone: v.timezone || '', device: v.device || '',
    browser: v.browser || '', os: v.os || '', suspectBot: v.is_bot === 1,
    referrer: v.referrer || '', firstPage: v.first_page || '', currentPage: v.current_page || '',
    pageViews: v.page_views || 0, visits: v.visits || 0,
    firstSeen: v.first_seen || '', lastSeen: v.last_seen || '', online,
    visitStartedAt: v.visit_started_at || '', dwellSec: v.dwell_sec || 0, currentDwellSec
  }
}

// ---- Paint formula library: shared helpers ----
// The imported data carries snowflake ids (up to 19 digits) that exceed
// JS Number's safe range. Statements over the formula tables therefore
// read BigInts, ids are serialised as strings, and id lookups bind the
// raw digit string — SQLite applies the column's INTEGER affinity, so
// comparisons and inserts stay exact.
function fq(sql) {
  const stmt = db.prepare(sql)
  if (typeof stmt.setReadBigInts === 'function') stmt.setReadBigInts(true)
  return stmt
}
function numOrStr(v) {
  if (typeof v !== 'bigint') return v
  return v <= 9007199254740991n && v >= -9007199254740991n ? Number(v) : String(v)
}
function camelKey(col) { return col.replace(/_([a-z])/g, (_, c) => c.toUpperCase()) }
function snakeKey(field) { return field.replace(/([A-Z])/g, '_$1').toLowerCase() }
// DB row -> API object for the formula tables: camelCase keys, id as a
// string, the enable char flag as a boolean, audit columns hidden.
function formulaRowOut(row) {
  const out = {}
  for (const [col, v] of Object.entries(row)) {
    if (col === 'is_deleted' || col === 'create_user_id' || col === 'update_user_id') continue
    const key = camelKey(col)
    out[key] = key === 'id' ? String(v) : numOrStr(v)
  }
  if ('enable' in out) out.enable = out.enable === '1' || out.enable === 1
  return out
}
// The source system stamps 'YYYY-MM-DD HH:mm:ss'; keep that shape for
// rows the admin creates in the formula tables.
function nowStamp() { return new Date().toISOString().slice(0, 19).replace('T', ' ') }

// Admin CRUD definitions for the formula dictionary tables. Field types:
// s = string (nullable), n = integer (nullable), f = float (nullable),
// b = enable flag stored as char '1'/'0'. `required` fields are enforced
// on create; `hasDeleted` tables hide is_deleted='1' rows from lists.
const FORMULA_CRUD = {
  'car-brands': {
    table: 'car_brand', order: 'letter_rank, id', hasDeleted: true,
    search: ['name'],
    fields: { name: 's', nameFirst: 's', country: 's', url: 's', letterRank: 'n', enable: 'b' }
  },
  'car-models': {
    table: 'car_model', order: 'sort_rank, id', hasDeleted: true,
    search: ['model', 'brand'], filters: { brandId: 'brand_id' },
    fields: { brand: 's', brandId: 'n', model: 's', sortRank: 'n', enable: 'b' },
    required: ['model']
  },
  'car-countries': {
    table: 'car_country', order: 'sort_rank, id', hasDeleted: false,
    search: ['name', 'name_en'],
    fields: { name: 's', nameEn: 's', language: 's', code: 'n', state: 'n', enable: 'b', sortRank: 'n' }
  },
  'car-factories': {
    table: 'car_factory', order: 'sort_rank, id', hasDeleted: false,
    search: ['name'], filters: { brandId: 'brand_id' },
    fields: { name: 's', brandId: 'n', country: 's', state: 'n', enable: 'b', sortRank: 'n' }
  },
  'color-cards': {
    table: 'color_card', order: 'id', hasDeleted: true,
    search: ['name', 'code'],
    fields: { name: 's', code: 's', enable: 'b' }
  },
  'masterbatches': {
    table: 'color_masterbatch', order: 'id', hasDeleted: true,
    search: ['model', 'name', 'eng_name'],
    fields: { model: 's', name: 's', engName: 's', type: 's', mark: 's', enable: 'b' },
    required: ['model', 'name', 'engName']
  },
  'car-colors': {
    table: 'car_color', order: 'id DESC', hasDeleted: true,
    search: ['car_color', 'color_num', 'factory_color', 'card_num'],
    filters: { brandId: 'brand_id', modelId: 'model_id' },
    fields: {
      brandId: 'n', modelId: 'n', carColor: 's', factoryColor: 's',
      colorNum: 's', cardNum: 's', carImage: 's', enable: 'b'
    },
    required: ['brandId', 'modelId']
  },
  'color-brands': {
    table: 'color_brand', order: 'id', hasDeleted: true,
    search: ['name'],
    fields: { name: 's', logo: 's', enable: 'b' }
  }
}
const FORMULA_NUMBER_CFG = {
  table: 'color_number', order: 'id', hasDeleted: true,
  search: ['name', 'color_name'],
  fields: { name: 's', colorName: 's', labL: 'f', labA: 'f', labB: 'f', enable: 'b' },
  required: ['name']
}

function formulaList(cfg, url) {
  const page = Math.max(1, Number(url.searchParams.get('page') || 1))
  const pageSize = Math.min(200, Math.max(1, Number(url.searchParams.get('pageSize') || 20)))
  const where = []
  const args = []
  if (cfg.hasDeleted) where.push("is_deleted = '0'")
  const keyword = String(url.searchParams.get('keyword') || '').trim()
  if (keyword && cfg.search.length) {
    where.push(`(${cfg.search.map((c) => `${c} LIKE ?`).join(' OR ')})`)
    for (let i = 0; i < cfg.search.length; i++) args.push(`%${keyword}%`)
  }
  for (const [param, col] of Object.entries(cfg.filters || {})) {
    const v = url.searchParams.get(param)
    if (v !== null && v !== '') { where.push(`${col} = ?`); args.push(v) }
  }
  const wsql = where.length ? ` WHERE ${where.join(' AND ')}` : ''
  const { n } = fq(`SELECT COUNT(*) AS n FROM ${cfg.table}${wsql}`).get(...args)
  const rows = fq(`SELECT * FROM ${cfg.table}${wsql} ORDER BY ${cfg.order} LIMIT ? OFFSET ?`)
    .all(...args, pageSize, (page - 1) * pageSize)
  return { list: rows.map(formulaRowOut), total: Number(n), page, pageSize }
}

// Pick the writable fields out of an admin payload, coerced per type.
// Returns { values } or { error } (missing required field on create).
function formulaValues(cfg, body, isCreate) {
  const values = {}
  for (const [field, type] of Object.entries(cfg.fields)) {
    if (body[field] === undefined) continue
    let v = body[field]
    if (type === 'b') v = v ? '1' : '0'
    // Long digit strings (snowflake ids used as foreign keys) stay
    // strings: SQLite applies the column's INTEGER affinity exactly,
    // while a detour through Number would corrupt them past 2^53.
    else if (type === 'n') v = v === '' || v === null ? null
      : (typeof v === 'string' && /^-?\d{16,}$/.test(v.trim()) ? v.trim() : Math.trunc(Number(v) || 0))
    else if (type === 'f') v = v === '' || v === null ? null : (Number(v) || 0)
    else v = v === null ? null : String(v)
    values[snakeKey(field)] = v
  }
  if (isCreate) {
    for (const field of cfg.required || []) {
      const v = values[snakeKey(field)]
      if (v === undefined || v === null || v === '') return { error: `${field} is required` }
    }
    if (values.enable === undefined && 'enable' in cfg.fields) values.enable = '1'
  }
  return { values }
}

function formulaInsert(cfg, values) {
  const stamp = nowStamp()
  const cols = { ...values }
  // Stamp audit times only where the source table carries them.
  const probe = fq(`SELECT name FROM pragma_table_info('${cfg.table}')`).all().map((r) => r.name)
  if (probe.includes('create_time') && cols.create_time === undefined) cols.create_time = stamp
  if (probe.includes('update_time') && cols.update_time === undefined) cols.update_time = stamp
  const names = Object.keys(cols)
  const info = fq(`INSERT INTO ${cfg.table} (${names.join(', ')}) VALUES (${names.map(() => '?').join(', ')})`)
    .run(...names.map((c) => cols[c]))
  return String(info.lastInsertRowid)
}

function formulaUpdate(cfg, id, values) {
  const probe = fq(`SELECT name FROM pragma_table_info('${cfg.table}')`).all().map((r) => r.name)
  const cols = { ...values }
  if (probe.includes('update_time')) cols.update_time = nowStamp()
  const names = Object.keys(cols)
  if (!names.length) return
  fq(`UPDATE ${cfg.table} SET ${names.map((c) => `${c} = ?`).join(', ')} WHERE id = ?`)
    .run(...names.map((c) => cols[c]), id)
}

function formulaGet(cfg, id) {
  return fq(`SELECT * FROM ${cfg.table} WHERE id = ?`).get(id)
}

// Geo for a tracking visitor: reuse the region already resolved for the
// same IP (another visitor row, or a chat session) before ever calling
// ip-api; only a cache miss goes to the network, fire-and-forget like
// the chat lookup so tracking never waits on it.
function resolveVisitorGeo(token, ip) {
  if (!ip || isPrivateIp(ip)) return
  const cached = db.prepare("SELECT country, region, city, timezone FROM visitors WHERE ip = ? AND country != '' AND token != ? LIMIT 1").get(ip, token)
    || db.prepare("SELECT country, region, city, timezone FROM chat_sessions WHERE ip = ? AND country != '' AND country != '本地/内网' LIMIT 1").get(ip)
  if (cached) {
    db.prepare('UPDATE visitors SET country = ?, region = ?, city = ?, timezone = ? WHERE token = ?')
      .run(cached.country, cached.region, cached.city, cached.timezone, token)
    return
  }
  const ctrl = new AbortController()
  const t = setTimeout(() => ctrl.abort(), 2500)
  fetch(`http://ip-api.com/json/${encodeURIComponent(ip)}?fields=status,country,regionName,city,timezone`, { signal: ctrl.signal })
    .then((r) => r.json())
    .then((g) => {
      if (g && g.status === 'success') {
        db.prepare('UPDATE visitors SET country = ?, region = ?, city = ?, timezone = ? WHERE token = ?')
          .run(String(g.country || ''), String(g.regionName || ''), String(g.city || ''), String(g.timezone || ''), token)
      }
    })
    .catch(() => { /* geo is best-effort only */ })
    .finally(() => clearTimeout(t))
}

// Local-day ISO boundaries for the "today" aggregates.
function localDayStartIso(offsetDays = 0) {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() + offsetDays)
  return d.toISOString()
}
function localDateKey(offsetDays = 0) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// Presence: a heartbeat (or message) within 45s counts as online; a typing
// stamp within 4s counts as "typing…".
function chatOnline(s) { return !!s.last_seen_at && Date.now() - Date.parse(s.last_seen_at) < 45000 }
function chatTyping(iso) { return !!iso && Date.now() - Date.parse(iso) < 4000 }

// Session profile as the admin console sees it (visitor internals such as
// the note stay admin-side; the visitor API never returns this object).
function chatSessionOut(s) {
  const { n } = db.prepare("SELECT COUNT(*) AS n FROM chat_messages WHERE session_id = ? AND sender = 'visitor'").get(s.id)
  return {
    id: s.id, visitorName: s.visitor_name, lang: s.lang,
    createdAt: s.created_at, lastMsgAt: s.last_msg_at,
    ip: s.ip || '', country: s.country || '', region: s.region || '', city: s.city || '',
    timezone: s.timezone || '', device: s.device || '', browser: s.browser || '', os: s.os || '',
    referrer: s.referrer || '', currentPage: s.current_page || '', pageViews: s.page_views || 0,
    lastSeenAt: s.last_seen_at || '', online: chatOnline(s),
    suspectBot: s.suspect_bot === 1, msgCount: n,
    status: s.status || 'open', note: s.note || '',
    visitorReadMsgId: s.visitor_read_msg_id || 0,
    visitorTyping: chatTyping(s.visitor_typing_at), agentTyping: chatTyping(s.agent_typing_at)
  }
}

function chatPageLabel(page, pageTitle) {
  const p = String(page || '').trim().slice(0, 300)
  const t = String(pageTitle || '').trim().slice(0, 120)
  return t ? `${p} · ${t}` : p
}

// ---- Admin: catalogue management + static files (admin UI, uploads) ----

const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json; charset=utf-8',
  '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg',
  '.webp': 'image/webp', '.gif': 'image/gif', '.svg': 'image/svg+xml', '.ico': 'image/x-icon',
  '.mp4': 'video/mp4', '.webm': 'video/webm', '.mov': 'video/quicktime'
}

function sendFile(res, filePath) {
  if (!existsSync(filePath)) return send(res, 404, { error: 'not found' })
  res.writeHead(200, { 'Content-Type': MIME[extname(filePath).toLowerCase()] || 'application/octet-stream' })
  res.end(readFileSync(filePath))
}

function parseJsonCol(text, fallback) {
  try { const v = JSON.parse(text || ''); return v && typeof v === 'object' ? v : fallback } catch { return fallback }
}

function adminBrandOut(row) {
  const { n } = db.prepare('SELECT COUNT(*) AS n FROM products WHERE brand_id = ?').get(row.id)
  return {
    id: row.id,
    names: parseJsonCol(row.names, {}),
    logo: row.logo || '',
    sort: row.sort || 0,
    active: row.active === 1,
    count: n
  }
}

function adminProductOut(row) {
  return {
    id: row.id,
    sourceId: row.source_id,
    categoryId: row.category_id,
    brandId: row.brand_id || '',
    names: parseJsonCol(row.names, {}),
    price: row.price,
    otPrice: row.ot_price,
    sales: row.sales,
    unit: row.unit,
    moq: row.moq,
    image: row.image,
    video: row.video || '',
    tag: row.tag,
    blurb: row.blurb,
    specs: parseJsonCol(row.specs, {}),
    sort: row.sort || 0,
    active: row.active === 1
  }
}

// Normalise an admin product payload into DB column values. Returns an
// error string instead when the payload is unusable.
function productValues(body) {
  const names = body.names && typeof body.names === 'object' ? body.names : {}
  if (!String(names.en || names.zh || '').trim()) return 'product name (English) is required'
  const brandId = String(body.brandId || '')
  if (brandId && !db.prepare('SELECT id FROM brands WHERE id = ?').get(brandId)) return 'brand not found'
  return {
    sourceId: String(body.sourceId ?? ''),
    categoryId: String(body.categoryId || ''),
    brandId,
    names: JSON.stringify(names),
    price: Number(body.price) || 0,
    otPrice: Number(body.otPrice) || 0,
    sales: Math.max(0, Math.trunc(Number(body.sales) || 0)),
    unit: String(body.unit || 'pc'),
    moq: Math.max(0, Math.trunc(Number(body.moq) || 0)),
    image: String(body.image || ''),
    video: String(body.video || ''),
    tag: String(body.tag || ''),
    blurb: String(body.blurb || ''),
    specs: JSON.stringify(body.specs && typeof body.specs === 'object' ? body.specs : {}),
    sort: Math.trunc(Number(body.sort) || 0),
    active: body.active === false ? 0 : 1
  }
}

// ---- Name auto-translate (admin data entry) ----
// Translation happens once, at data-entry time: the admin form sends the
// Chinese name and gets the other languages back to store in the names JSON — the
// storefront keeps reading the DB, nothing is translated at runtime.
// Default provider is the self-hosted LibreTranslate (LIBRETRANSLATE_URL,
// default http://127.0.0.1:5000 — see docker-compose.yml). The free
// MyMemory API is kept as an explicit opt-in fallback:
// TRANSLATE_PROVIDER=mymemory (set TRANSLATE_EMAIL to raise its quota).
// A request may override the provider for that call only via
// `provider: "mymemory" | "libre"` (the admin batch translate pins
// MyMemory this way); any other value is a 400. There is NO silent
// fallback: if the chosen provider does not support a language, that
// language lands in `errors` like any other failure.
// Supported target languages (source is always zh). Keep in step with the
// admin language registry (taojie-overseas-admin/src/utils/langs.js).
const TRANSLATE_TARGETS = ['en', 'es', 'fr', 'pt', 'id', 'ms', 'vi', 'th', 'ar', 'ru', 'de']
const TRANSLATE_TIMEOUT_MS = 6000

function decodeHtmlEntities(s) {
  return String(s)
    .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(Number(n)))
    .replace(/&#x([0-9a-fA-F]+);/g, (_, n) => String.fromCodePoint(parseInt(n, 16)))
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
}

// Sentences MyMemory puts in the translation slot when something is
// wrong (quota exhausted, bad query, ...). responseStatus normally
// flags these too, but never let one be stored as a name.
const MYMEMORY_WARNING_RE = /MYMEMORY WARNING|YOU USED ALL AVAILABLE FREE TRANSLATIONS|QUERY LENGTH LIMIT EXCEEDED|NO QUERY SPECIFIED|INVALID (TARGET|SOURCE) LANGUAGE|INVALID EMAIL/i

async function translateViaMyMemory(text, target) {
  const pair = encodeURIComponent(`zh-CN|${target}`)
  let u = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=${pair}`
  if (process.env.TRANSLATE_EMAIL) u += `&de=${encodeURIComponent(process.env.TRANSLATE_EMAIL)}`
  const res = await fetch(u, { signal: AbortSignal.timeout(TRANSLATE_TIMEOUT_MS) })
  // Quota exhaustion can also surface as a bare HTTP 429/403 with no
  // usable body — that is an error, never a translation.
  if (!res.ok) throw new Error(`mymemory HTTP ${res.status}`)
  const data = await res.json()
  // On quota exhaustion MyMemory still returns 200-shaped JSON but with
  // responseStatus 403 and a warning sentence as the "translation".
  if (!data || Number(data.responseStatus) !== 200) {
    throw new Error(`mymemory: ${(data && data.responseDetails) || 'quota or upstream error'}`)
  }
  const out = data.responseData && data.responseData.translatedText
  if (!out) throw new Error('mymemory: empty result')
  const cleaned = decodeHtmlEntities(out).trim()
  if (MYMEMORY_WARNING_RE.test(cleaned)) throw new Error(`mymemory: ${cleaned}`)
  // Optional passthrough for the admin MyMemory query page: only the
  // fields the source response actually carries, never invented quota
  // numbers (MyMemory has no remaining-quota API).
  const meta = { responseStatus: Number(data.responseStatus) }
  const quotaFinished = data.quotaFinished !== undefined ? data.quotaFinished
    : (data.responseData && data.responseData.quotaFinished)
  if (typeof quotaFinished === 'boolean') meta.quotaFinished = quotaFinished
  return { text: cleaned, meta }
}

async function translateViaLibre(text, target) {
  const base = (process.env.LIBRETRANSLATE_URL || 'http://127.0.0.1:5000').replace(/\/$/, '')
  const payload = { q: text, source: 'zh', target, format: 'text' }
  const libreKey = process.env['LIBRETRANSLATE_' + 'API_KEY']
  if (libreKey) payload.api_key = libreKey
  const res = await fetch(`${base}/translate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(TRANSLATE_TIMEOUT_MS)
  })
  if (!res.ok) throw new Error(`libretranslate HTTP ${res.status}`)
  const data = await res.json()
  if (!data || !data.translatedText) throw new Error('libretranslate: empty result')
  return { text: decodeHtmlEntities(data.translatedText).trim() }
}

const TRANSLATE_PROVIDERS = ['mymemory', 'libre']

// Provider for one request: an explicit body `provider` wins over the
// environment; absent means the env default (TRANSLATE_PROVIDER=mymemory,
// otherwise the self-hosted LibreTranslate). Returns null for a value
// outside the allowlist — the handler turns that into a 400.
function resolveTranslateProvider(body) {
  const raw = body && body.provider !== undefined && body.provider !== null ? String(body.provider).trim() : ''
  if (!raw) return process.env.TRANSLATE_PROVIDER === 'mymemory' ? 'mymemory' : 'libre'
  const p = raw.toLowerCase()
  return TRANSLATE_PROVIDERS.includes(p) ? p : null
}

async function translateOne(text, target, provider) {
  if (provider === 'mymemory') {
    return translateViaMyMemory(text, target)
  }
  return translateViaLibre(text, target)
}

// ---- FX rates for the admin price calculator ----
// Source: open.er-api.com (free, no API key, ~160 currencies, updated
// daily). Rates are cached per base currency in the fx_rates table for
// 12 hours; when a refresh fails, the stale cache is served (flagged
// `stale`) instead of failing the tool — only "no cache + fetch failed"
// is a 502. All conversion math happens in the admin page.
const FX_TTL_MS = 12 * 60 * 60 * 1000
const FX_TIMEOUT_MS = 6000

async function fetchFxRates(base) {
  const res = await fetch(`https://open.er-api.com/v6/latest/${base}`, {
    signal: AbortSignal.timeout(FX_TIMEOUT_MS)
  })
  if (!res.ok) throw new Error(`er-api HTTP ${res.status}`)
  const data = await res.json()
  if (!data || data.result !== 'success' || !data.rates || typeof data.rates !== 'object') {
    throw new Error('er-api: unexpected response')
  }
  const sourceUpdatedAt = data.time_last_update_unix
    ? new Date(data.time_last_update_unix * 1000).toISOString()
    : String(data.time_last_update_utc || '')
  return { rates: data.rates, sourceUpdatedAt }
}

async function getFxRates(base) {
  const row = db.prepare('SELECT * FROM fx_rates WHERE base = ?').get(base)
  const cached = row
    ? { base, rates: parseJsonCol(row.rates, {}), sourceUpdatedAt: row.source_updated_at, fetchedAt: row.fetched_at }
    : null
  if (cached && Date.now() - Date.parse(cached.fetchedAt) < FX_TTL_MS) {
    return { ...cached, stale: false, fromCache: true }
  }
  try {
    const fresh = await fetchFxRates(base)
    const fetchedAt = new Date().toISOString()
    db.prepare(`INSERT INTO fx_rates (base, rates, source_updated_at, fetched_at) VALUES (?, ?, ?, ?)
      ON CONFLICT(base) DO UPDATE SET rates = excluded.rates,
        source_updated_at = excluded.source_updated_at, fetched_at = excluded.fetched_at`)
      .run(base, JSON.stringify(fresh.rates), fresh.sourceUpdatedAt, fetchedAt)
    return { base, rates: fresh.rates, sourceUpdatedAt: fresh.sourceUpdatedAt, fetchedAt, stale: false, fromCache: false }
  } catch (e) {
    if (cached) return { ...cached, stale: true, fromCache: true }
    throw e
  }
}

// Shared by GET /api/admin/fx/rates (login-gated price calculator) and
// GET /api/fx/rates (public, storefront currency switcher): one
// validation + cache path, identical response shape.
async function sendFxRates(res, url) {
  const base = String(url.searchParams.get('base') || 'CNY').toUpperCase()
  if (!/^[A-Z]{3}$/.test(base)) return send(res, 400, { error: 'base must be a 3-letter currency code' })
  try {
    return send(res, 200, await getFxRates(base))
  } catch {
    return send(res, 502, { error: 'exchange rate service unavailable' })
  }
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost')
  const path = url.pathname.replace(/\/$/, '') || '/'
  const lang = LANGS.includes(url.searchParams.get('lang')) ? url.searchParams.get('lang') : 'en'
  try {
    if (req.method === 'OPTIONS') return send(res, 204, {})

    // Admin gate: /api/admin/login is public; every other admin path needs
    // a root token or a login session, plus the permission for that area.
    if (path.startsWith('/api/admin/') && path !== '/api/admin/login') {
      const auth = resolveAuth(req)
      if (!auth) return send(res, 401, { error: 'login required' })
      const need = requiredPerm(path)
      // Product editors need the category list to fill the category dropdown.
      const categoryReadByProduct = need === 'categories' && req.method === 'GET' && hasPerm(auth.permissions, 'products')
      if (need && !categoryReadByProduct && !hasPerm(auth.permissions, need)) {
        return send(res, 403, { error: `no permission: ${need}` })
      }
      req.adminAuth = auth
    }

    if (req.method === 'GET' && path === '/api/health') {
      const { n } = db.prepare('SELECT COUNT(*) AS n FROM products WHERE active = 1').get()
      return send(res, 200, { ok: true, products: n })
    }

    if (req.method === 'GET' && path === '/api/categories') {
      const rows = db.prepare('SELECT * FROM categories ORDER BY sort, id').all()
      const counts = db.prepare('SELECT category_id, COUNT(*) AS n FROM products WHERE active = 1 GROUP BY category_id').all()
      const byId = Object.fromEntries(counts.map((r) => [r.category_id, r.n]))
      return send(res, 200, rows.map((c) => ({ id: c.id, name: pickLang(c.names, lang), count: byId[c.id] || 0 })))
    }

    if (req.method === 'GET' && path === '/api/products') {
      const keyword = (url.searchParams.get('keyword') || '').trim().toLowerCase()
      const categoryId = url.searchParams.get('categoryId') || ''
      const brandId = url.searchParams.get('brandId') || ''
      const page = Math.max(1, Number(url.searchParams.get('page') || 1))
      const limit = Math.min(100, Math.max(1, Number(url.searchParams.get('limit') || 48)))
      let rows = db.prepare('SELECT * FROM products WHERE active = 1 ORDER BY sort, id').all()
      if (categoryId) rows = rows.filter((r) => r.category_id === categoryId)
      if (brandId) rows = rows.filter((r) => String(r.brand_id || '') === String(brandId))
      if (keyword) {
        rows = rows.filter((r) => {
          const names = JSON.parse(r.names || '{}')
          const cat = db.prepare('SELECT names FROM categories WHERE id = ?').get(r.category_id)
          const hay = [...Object.values(names), cat ? pickLang(cat.names, 'en') : '', r.blurb].join(' ').toLowerCase()
          return hay.includes(keyword)
        })
      }
      const total = rows.length
      const items = rows.slice((page - 1) * limit, page * limit).map((r) => productOut(r, lang))
      return send(res, 200, { list: items, total, page, limit })
    }

    const detailMatch = path.match(/^\/api\/products\/(\d+)$/)
    if (req.method === 'GET' && detailMatch) {
      const row = db.prepare('SELECT * FROM products WHERE id = ? AND active = 1').get(Number(detailMatch[1]))
      if (!row) return send(res, 404, { error: 'product not found' })
      return send(res, 200, productOut(row, lang))
    }

    if (req.method === 'GET' && path === '/api/keywords') {
      return send(res, 200, ['clear coat', 'primer', 'pearl', 'basecoat', 'tissue', 'kettle'])
    }

    if (req.method === 'GET' && path === '/api/site-config') {
      return send(res, 200, getConfig())
    }

    // Public FX rates for the storefront currency switcher — same
    // fetch + 12h cache as the admin calculator, no login required.
    if (req.method === 'GET' && path === '/api/fx/rates') {
      return sendFxRates(res, url)
    }

    if (req.method === 'PUT' && path === '/api/admin/site-config') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const body = await readBody(req)
      const merged = { ...getConfig() }
      for (const k of Object.keys(DEFAULT_CONFIG)) if (body[k] !== undefined) merged[k] = body[k]
      // navItems must stay an array of {label, link, type?, categoryId?};
      // drop anything else. type is whitelisted so a junk value cannot
      // reach the storefront's click dispatcher.
      if (!Array.isArray(merged.navItems)) merged.navItems = []
      merged.navItems = merged.navItems
        .filter((i) => i && typeof i === 'object')
        .map((i) => {
          const item = { label: String(i.label || ''), link: String(i.link || '') }
          const type = String(i.type || '')
          if (type === 'category' || type === 'anchor' || type === 'external') item.type = type
          if (i.categoryId !== undefined && i.categoryId !== null && String(i.categoryId) !== '') {
            item.categoryId = String(i.categoryId)
          }
          return item
        })
      db.prepare('INSERT INTO site_config (id, data) VALUES (1, ?) ON CONFLICT(id) DO UPDATE SET data = excluded.data')
        .run(JSON.stringify(merged))
      return send(res, 200, merged)
    }

    // Name auto-translate for the admin forms. The admin gate above has
    // already required a login session or the root token (no specific menu
    // permission — products, categories and brands all use it). A single
    // language failing lands in `errors`; only a total failure is a 502.
    if (req.method === 'POST' && path === '/api/admin/translate') {
      const body = await readBody(req)
      const text = String(body.text || '').trim()
      if (!text) return send(res, 400, { error: 'text is required' })
      if (text.length > 500) return send(res, 400, { error: 'text too long (max 500 chars)' })
      // Per-request provider override (see resolveTranslateProvider);
      // an unknown provider is a client error, never a silent default.
      const provider = resolveTranslateProvider(body)
      if (!provider) return send(res, 400, { error: 'provider must be "mymemory" or "libre"' })
      const wanted = Array.isArray(body.targets) && body.targets.length ? body.targets : TRANSLATE_TARGETS
      const requested = [...new Set(wanted.map((t) => String(t)).filter(Boolean))]
      const targets = requested.filter((t) => TRANSLATE_TARGETS.includes(t))
      const translations = {}
      const errors = {}
      let meta
      // Codes outside the supported list are never fetched, just reported.
      for (const t of requested) if (!TRANSLATE_TARGETS.includes(t)) errors[t] = 'unsupported'
      if (!targets.length) {
        return send(res, 502, { error: 'translation service unavailable', errors, provider })
      }
      await Promise.all(targets.map(async (t) => {
        try {
          const r = await translateOne(text, t, provider)
          translations[t] = r.text
          if (!meta && r.meta) meta = r.meta
        } catch (e) {
          errors[t] = String((e && e.message) || e)
        }
      }))
      if (!Object.keys(translations).length) {
        return send(res, 502, { error: 'translation service unavailable', errors, provider })
      }
      const out = { translations, errors, provider }
      // meta exists only for MyMemory calls whose source response
      // carried quota fields (see translateViaMyMemory).
      if (meta) out.meta = meta
      return send(res, 200, out)
    }

    // FX rates for the admin price calculator. The admin gate above has
    // already required a login session or the root token (no specific
    // menu permission — requiredPerm returns null for this path, same as
    // the translate endpoint, so sales staff can use it too).
    if (req.method === 'GET' && path === '/api/admin/fx/rates') {
      return sendFxRates(res, url)
    }

    if (req.method === 'POST' && path === '/api/rfq') {
      const ip = (req.headers['x-forwarded-for'] || req.socket.remoteAddress || '').toString().split(',')[0].trim()
      if (!rfqAllowed(ip)) return send(res, 429, { error: 'too many requests, try again later' })
      const body = await readBody(req)
      const name = String(body.name || '').trim()
      const contact = String(body.contact || '').trim()
      if (!name || !contact) return send(res, 400, { error: 'name and contact are required' })
      const info = db.prepare(`INSERT INTO rfq (name, contact, product, qty, message, lang, ip, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)`).run(
        name, contact, String(body.product || '').trim(), String(body.qty || '').trim(),
        String(body.message || '').trim(), LANGS.includes(body.lang) ? body.lang : 'en',
        ip, new Date().toISOString())
      // Notification hook: point RFQ_WEBHOOK_URL at your mail/WhatsApp gateway.
      const hook = process.env.RFQ_WEBHOOK_URL
      if (hook) {
        fetch(hook, {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ type: 'rfq', id: Number(info.lastInsertRowid), name, contact, product: String(body.product || ''), qty: String(body.qty || ''), message: String(body.message || '') })
        }).catch(() => { /* notification failure must not fail the submission */ })
      }
      return send(res, 201, { ok: true, id: Number(info.lastInsertRowid) })
    }

    if (req.method === 'GET' && path === '/api/admin/rfq') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const rows = db.prepare('SELECT * FROM rfq ORDER BY id DESC LIMIT 200').all()
      return send(res, 200, rows)
    }

    // ---- Online chat: visitor endpoints (identity = the session token) ----
    if (req.method === 'POST' && path === '/api/chat/start') {
      const body = await readBody(req)
      const ip = clientIpOf(req)
      const nowIso = new Date().toISOString()
      const existing = chatSessionByToken(String(body.token || req.headers['x-chat-token'] || '').trim())
      if (existing) {
        // Reuse: refresh presence, and follow the visitor to a new page.
        const page = chatPageLabel(body.page, body.pageTitle)
        if (page && page !== existing.current_page) {
          db.prepare('UPDATE chat_sessions SET last_seen_at = ?, current_page = ?, page_views = page_views + 1 WHERE id = ?')
            .run(nowIso, page, existing.id)
        } else {
          db.prepare('UPDATE chat_sessions SET last_seen_at = ? WHERE id = ?').run(nowIso, existing.id)
        }
        return send(res, 200, { sessionId: existing.id, token: existing.visitor_token, name: existing.visitor_name })
      }
      if (!hitAllowed(chatStartHits, ip, 10)) return send(res, 429, { error: 'too many requests, try again later' })
      const name = String(body.name || '').trim().slice(0, 64) || '访客'
      const token = randomBytes(24).toString('hex')
      const ua = parseUserAgent(req.headers['user-agent'])
      const local = isPrivateIp(ip)
      const info = db.prepare(`INSERT INTO chat_sessions
        (visitor_token, visitor_name, lang, created_at, last_msg_at, ip, country, device, browser, os,
         referrer, current_page, page_views, last_seen_at, suspect_bot, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, 'open')`)
        .run(token, name, LANGS.includes(body.lang) ? body.lang : 'en', nowIso, nowIso,
          ip, local ? '本地/内网' : '', ua.device, ua.browser, ua.os,
          String(body.referrer || '').trim().slice(0, 500), chatPageLabel(body.page, body.pageTitle),
          nowIso, ua.suspectBot ? 1 : 0)
      const sessionId = Number(info.lastInsertRowid)
      if (!local) lookupChatGeo(sessionId, ip)
      return send(res, 201, { sessionId, token, name })
    }

    if (path === '/api/chat/messages' && req.method === 'POST') {
      const body = await readBody(req)
      const session = chatSessionByToken(String(body.token || '').trim())
      if (!session) return send(res, 401, { error: 'chat session not found' })
      const text = String(body.body || '').trim()
      if (!text) return send(res, 400, { error: 'message is empty' })
      const ip = clientIpOf(req)
      if (!hitAllowed(chatMsgHits, ip, 30)) return send(res, 429, { error: 'too many requests, try again later' })
      // A message to a closed session re-opens it.
      if (session.status === 'closed') {
        db.prepare("UPDATE chat_sessions SET status = 'open' WHERE id = ?").run(session.id)
      }
      return send(res, 201, insertChatMessage(session.id, 'visitor', text.slice(0, 2000)))
    }

    if (path === '/api/chat/messages' && req.method === 'GET') {
      const session = chatSessionByToken(String(url.searchParams.get('token') || '').trim())
      if (!session) return send(res, 401, { error: 'chat session not found' })
      const afterId = Math.max(0, Number(url.searchParams.get('afterId') || 0))
      const rows = db.prepare('SELECT * FROM chat_messages WHERE session_id = ? AND id > ? ORDER BY id').all(session.id, afterId)
      // Pulling up to the latest message means the visitor has now seen
      // every agent message there is: advance their read watermark.
      const { maxAgent } = db.prepare("SELECT COALESCE(MAX(id), 0) AS maxAgent FROM chat_messages WHERE session_id = ? AND sender = 'agent'").get(session.id)
      if (maxAgent > (session.visitor_read_msg_id || 0)) {
        db.prepare('UPDATE chat_sessions SET visitor_read_msg_id = ? WHERE id = ?').run(maxAgent, session.id)
      }
      return send(res, 200, {
        sessionId: session.id, name: session.visitor_name,
        messages: rows.map(chatMessageOut), agentTyping: chatTyping(session.agent_typing_at)
      })
    }

    // Visitor heartbeat: keeps the online flag warm and follows page moves.
    if (req.method === 'POST' && path === '/api/chat/ping') {
      const body = await readBody(req)
      const session = chatSessionByToken(String(body.token || '').trim())
      if (!session) return send(res, 401, { error: 'chat session not found' })
      const nowIso = new Date().toISOString()
      const page = chatPageLabel(body.page, body.pageTitle)
      if (page && page !== session.current_page) {
        db.prepare('UPDATE chat_sessions SET last_seen_at = ?, current_page = ?, page_views = page_views + 1 WHERE id = ?')
          .run(nowIso, page, session.id)
      } else {
        db.prepare('UPDATE chat_sessions SET last_seen_at = ? WHERE id = ?').run(nowIso, session.id)
      }
      return send(res, 200, { ok: true })
    }

    // Visitor typing signal (the widget throttles this to one per 2s).
    if (req.method === 'POST' && path === '/api/chat/typing') {
      const body = await readBody(req)
      const session = chatSessionByToken(String(body.token || '').trim())
      if (!session) return send(res, 401, { error: 'chat session not found' })
      const nowIso = new Date().toISOString()
      db.prepare('UPDATE chat_sessions SET visitor_typing_at = ?, last_seen_at = ? WHERE id = ?').run(nowIso, nowIso, session.id)
      return send(res, 200, { ok: true })
    }

    // ---- Visitor statistics: storefront beacon ----
    // type=view (default) counts a page view; type=click records what
    // the visitor clicked without counting a view; type=heartbeat only
    // keeps presence/dwell warm. Rate limit: 120/min per IP (a page
    // view + 30s heartbeats stay far below it); over the limit gets a
    // plain 429, same style as the chat endpoints.
    if (req.method === 'POST' && path === '/api/track') {
      const body = await readBody(req)
      const ip = clientIpOf(req)
      if (!hitAllowed(trackHits, ip, 120)) return send(res, 429, { error: 'too many requests, try again later' })
      const type = ['view', 'heartbeat', 'click'].includes(String(body.type || '')) ? String(body.type) : 'view'
      let token = String(body.token || '').trim()
      if (!TRACK_TOKEN_RE.test(token)) token = randomBytes(24).toString('hex')
      const page = String(body.page || '').trim().slice(0, 300)
      const action = String(body.action || '').trim().slice(0, 200)
      const nowMs = Date.now()
      const nowIso = new Date(nowMs).toISOString()
      const existing = visitorByToken(token)
      if (!existing) {
        const ua = parseUserAgent(req.headers['user-agent'])
        const local = isPrivateIp(ip)
        db.prepare(`INSERT INTO visitors
          (token, ip, country, device, browser, os, is_bot, referrer, first_page, current_page,
           page_views, visits, first_seen, last_seen, visit_started_at, dwell_sec)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, 0)`)
          .run(token, ip, local ? '本地/内网' : '', ua.device, ua.browser, ua.os, ua.suspectBot ? 1 : 0,
            String(body.referrer || '').trim().slice(0, 500), page, page,
            type === 'view' ? 1 : 0, nowIso, nowIso, nowIso)
        if (type === 'view') {
          db.prepare('INSERT INTO visit_events (token, kind, page, action, created_at) VALUES (?, ?, ?, ?, ?)')
            .run(token, 'view', page, '', nowIso)
        } else if (type === 'click') {
          db.prepare('INSERT INTO visit_events (token, kind, page, action, created_at) VALUES (?, ?, ?, ?, ?)')
            .run(token, 'click', page, action, nowIso)
        }
        if (!local) resolveVisitorGeo(token, ip)
        return send(res, 200, { ok: true, token })
      }
      const acc = accrueVisit(existing, nowMs)
      const ua = parseUserAgent(req.headers['user-agent'])
      const ipChanged = ip && ip !== existing.ip
      db.prepare(`UPDATE visitors SET ip = ?, device = ?, browser = ?, os = ?, is_bot = ?,
          referrer = CASE WHEN referrer = '' THEN ? ELSE referrer END,
          current_page = ?, last_seen = ?, visits = ?, dwell_sec = ?, visit_started_at = ?
          ${type === 'view' ? ', page_views = page_views + 1' : ''}
          WHERE token = ?`)
        .run(ip || existing.ip, ua.device, ua.browser, ua.os, ua.suspectBot ? 1 : 0,
          String(body.referrer || '').trim().slice(0, 500),
          page || existing.current_page, nowIso, acc.visits, acc.dwellSec, acc.visitStarted, token)
      if (type === 'view') {
        db.prepare('INSERT INTO visit_events (token, kind, page, action, created_at) VALUES (?, ?, ?, ?, ?)')
          .run(token, 'view', page, '', nowIso)
      } else if (type === 'click') {
        db.prepare('INSERT INTO visit_events (token, kind, page, action, created_at) VALUES (?, ?, ?, ?, ?)')
          .run(token, 'click', page || existing.current_page, action, nowIso)
      }
      if (ipChanged || !existing.country) resolveVisitorGeo(token, ip || existing.ip)
      return send(res, 200, { ok: true, token })
    }

    // ---- Admin: visitor statistics ----
    if (req.method === 'GET' && path === '/api/admin/visitors') {
      const nowMs = Date.now()
      const onlineSince = new Date(nowMs - ONLINE_MS).toISOString()
      const { n: online } = db.prepare('SELECT COUNT(*) AS n FROM visitors WHERE last_seen >= ?').get(onlineSince)
      const todayStart = localDayStartIso(0)
      const tomorrowStart = localDayStartIso(1)
      const today = db.prepare("SELECT COUNT(*) AS pv, COUNT(DISTINCT token) AS uv FROM visit_events WHERE kind = 'view' AND created_at >= ? AND created_at < ?")
        .get(todayStart, tomorrowStart)
      const { n: totalUv } = db.prepare('SELECT COUNT(*) AS n FROM visitors').get()
      const { pv: totalPv } = db.prepare('SELECT COALESCE(SUM(page_views), 0) AS pv FROM visitors').get()
      const countries = db.prepare(`SELECT CASE WHEN country = '' THEN '未知' ELSE country END AS country,
          COUNT(*) AS visitors FROM visitors GROUP BY country ORDER BY visitors DESC LIMIT 15`).all()
      const trend = []
      for (let back = 6; back >= 0; back--) {
        const row = db.prepare("SELECT COUNT(*) AS pv, COUNT(DISTINCT token) AS uv FROM visit_events WHERE kind = 'view' AND created_at >= ? AND created_at < ?")
          .get(localDayStartIso(-back), localDayStartIso(-back + 1))
        trend.push({ date: localDateKey(-back), uv: row.uv, pv: row.pv })
      }
      const rows = db.prepare('SELECT * FROM visitors ORDER BY last_seen DESC LIMIT 100').all()
      return send(res, 200, {
        summary: { online, todayUv: today.uv, todayPv: today.pv, totalUv, totalPv },
        countries, trend, visitors: rows.map(visitorOut)
      })
    }

    // One visitor's timeline (views + clicks, newest first) for the
    // admin detail drawer.
    const visitorEventsMatch = path.match(/^\/api\/admin\/visitors\/([^/]+)\/events$/)
    if (req.method === 'GET' && visitorEventsMatch) {
      const token = decodeURIComponent(visitorEventsMatch[1])
      if (!visitorByToken(token)) return send(res, 404, { error: 'visitor not found' })
      const limit = Math.min(200, Math.max(1, Number(url.searchParams.get('limit') || 50)))
      const rows = db.prepare('SELECT * FROM visit_events WHERE token = ? ORDER BY id DESC LIMIT ?').all(token, limit)
      return send(res, 200, rows.map((e) => ({
        id: e.id, kind: e.kind, page: e.page || '', action: e.action || '', createdAt: e.created_at
      })))
    }

    // ---- Admin: online chat (list sessions, read a thread, reply) ----
    if (path === '/api/admin/chat/sessions' && req.method === 'GET') {
      const keyword = String(url.searchParams.get('keyword') || '').trim()
      let rows
      if (keyword) {
        const like = `%${keyword}%`
        rows = db.prepare(`SELECT * FROM chat_sessions
          WHERE visitor_name LIKE ? OR id IN (SELECT session_id FROM chat_messages WHERE body LIKE ?)
          ORDER BY last_msg_at DESC, id DESC`).all(like, like)
      } else {
        rows = db.prepare('SELECT * FROM chat_sessions ORDER BY last_msg_at DESC, id DESC').all()
      }
      return send(res, 200, rows.map((s) => {
        const last = db.prepare('SELECT body FROM chat_messages WHERE session_id = ? ORDER BY id DESC LIMIT 1').get(s.id)
        const { n } = db.prepare("SELECT COUNT(*) AS n FROM chat_messages WHERE session_id = ? AND sender = 'visitor' AND id > ?")
          .get(s.id, s.last_agent_read_msg_id)
        return { ...chatSessionOut(s), lastMessage: last ? last.body : '', unread: n }
      }))
    }
    const adminChatMsgMatch = path.match(/^\/api\/admin\/chat\/sessions\/(\d+)\/messages$/)
    if (adminChatMsgMatch && req.method === 'GET') {
      const id = Number(adminChatMsgMatch[1])
      const session = db.prepare('SELECT * FROM chat_sessions WHERE id = ?').get(id)
      if (!session) return send(res, 404, { error: 'chat session not found' })
      const rows = db.prepare('SELECT * FROM chat_messages WHERE session_id = ? ORDER BY id').all(id)
      // Opening the thread marks every visitor message in it as read.
      const maxId = rows.length ? rows[rows.length - 1].id : 0
      if (maxId > session.last_agent_read_msg_id) {
        db.prepare('UPDATE chat_sessions SET last_agent_read_msg_id = ? WHERE id = ?').run(maxId, id)
        session.last_agent_read_msg_id = maxId
      }
      return send(res, 200, { session: chatSessionOut(session), messages: rows.map(chatMessageOut) })
    }
    if (adminChatMsgMatch && req.method === 'POST') {
      const id = Number(adminChatMsgMatch[1])
      const session = db.prepare('SELECT * FROM chat_sessions WHERE id = ?').get(id)
      if (!session) return send(res, 404, { error: 'chat session not found' })
      const body = await readBody(req)
      const text = String(body.body || '').trim()
      if (!text) return send(res, 400, { error: 'message is empty' })
      db.prepare("UPDATE chat_sessions SET agent_typing_at = '' WHERE id = ?").run(id)
      return send(res, 201, insertChatMessage(id, 'agent', text.slice(0, 2000)))
    }
    // Agent typing signal (the console throttles this to one per 2s).
    const adminChatTypingMatch = path.match(/^\/api\/admin\/chat\/sessions\/(\d+)\/typing$/)
    if (adminChatTypingMatch && req.method === 'POST') {
      const id = Number(adminChatTypingMatch[1])
      if (!db.prepare('SELECT id FROM chat_sessions WHERE id = ?').get(id)) return send(res, 404, { error: 'chat session not found' })
      db.prepare('UPDATE chat_sessions SET agent_typing_at = ? WHERE id = ?').run(new Date().toISOString(), id)
      return send(res, 200, { ok: true })
    }
    // Internal note — agents only, never sent to the visitor.
    const adminChatNoteMatch = path.match(/^\/api\/admin\/chat\/sessions\/(\d+)\/note$/)
    if (adminChatNoteMatch && req.method === 'PUT') {
      const id = Number(adminChatNoteMatch[1])
      if (!db.prepare('SELECT id FROM chat_sessions WHERE id = ?').get(id)) return send(res, 404, { error: 'chat session not found' })
      const body = await readBody(req)
      const note = String(body.note || '').slice(0, 2000)
      db.prepare('UPDATE chat_sessions SET note = ? WHERE id = ?').run(note, id)
      return send(res, 200, { ok: true, note })
    }
    // Lifecycle: open <-> closed (a visitor message re-opens automatically).
    const adminChatStatusMatch = path.match(/^\/api\/admin\/chat\/sessions\/(\d+)\/status$/)
    if (adminChatStatusMatch && req.method === 'PUT') {
      const id = Number(adminChatStatusMatch[1])
      if (!db.prepare('SELECT id FROM chat_sessions WHERE id = ?').get(id)) return send(res, 404, { error: 'chat session not found' })
      const body = await readBody(req)
      const status = body.status === 'closed' ? 'closed' : 'open'
      db.prepare('UPDATE chat_sessions SET status = ? WHERE id = ?').run(status, id)
      return send(res, 200, { ok: true, status })
    }

    // ---- Paint formula library: public search ----
    // Mirrors the source system's front APIs (homeSearch / brand and
    // model pickers / colour detail + formula datas) in this backend's
    // REST shape. Only enabled, non-deleted rows are visible.
    if (req.method === 'GET' && path === '/api/formula/brands') {
      const rows = fq("SELECT id, name FROM car_brand WHERE enable = '1' AND is_deleted = '0' ORDER BY letter_rank, id").all()
      return send(res, 200, rows.map((r) => ({ id: String(r.id), name: r.name || '' })))
    }
    if (req.method === 'GET' && path === '/api/formula/models') {
      const brandId = url.searchParams.get('brandId') || ''
      const rows = brandId
        ? fq("SELECT id, brand_id, model FROM car_model WHERE brand_id = ? AND enable = '1' AND is_deleted = '0' ORDER BY sort_rank, id").all(brandId)
        : fq("SELECT id, brand_id, model FROM car_model WHERE enable = '1' AND is_deleted = '0' ORDER BY sort_rank, id").all()
      return send(res, 200, rows.map((r) => ({ id: String(r.id), brandId: r.brand_id === null ? '' : String(r.brand_id), model: r.model || '' })))
    }
    if (req.method === 'GET' && path === '/api/formula/search') {
      const brandId = url.searchParams.get('brandId') || ''
      const modelId = url.searchParams.get('modelId') || ''
      const keyword = String(url.searchParams.get('keyword') || '').trim()
      const page = Math.max(1, Number(url.searchParams.get('page') || 1))
      const pageSize = Math.min(100, Math.max(1, Number(url.searchParams.get('pageSize') || 15)))
      const where = ["cc.enable = '1'", "cc.is_deleted = '0'"]
      const args = []
      if (brandId) { where.push('cc.brand_id = ?'); args.push(brandId) }
      if (modelId) { where.push('cc.model_id = ?'); args.push(modelId) }
      if (keyword) {
        where.push('(cc.car_color LIKE ? OR cc.color_num LIKE ? OR cc.factory_color LIKE ? OR cc.card_num LIKE ?)')
        for (let i = 0; i < 4; i++) args.push(`%${keyword}%`)
      }
      const wsql = ` WHERE ${where.join(' AND ')}`
      const { n } = fq(`SELECT COUNT(*) AS n FROM car_color cc${wsql}`).get(...args)
      const rows = fq(`SELECT cc.*, b.name AS brand_name, m.model AS model_name
          FROM car_color cc
          LEFT JOIN car_brand b ON b.id = cc.brand_id
          LEFT JOIN car_model m ON m.id = cc.model_id
          ${wsql} ORDER BY cc.update_time DESC, cc.id DESC LIMIT ? OFFSET ?`)
        .all(...args, pageSize, (page - 1) * pageSize)
      return send(res, 200, {
        items: rows.map((r) => ({
          id: String(r.id),
          brandId: String(r.brand_id), brandName: r.brand_name || '',
          modelId: String(r.model_id), modelName: r.model_name || '',
          carColor: r.car_color || '', factoryColor: r.factory_color || '',
          colorNum: r.color_num || '', cardNum: r.card_num || '',
          carImage: r.car_image || '', updateTime: r.update_time || ''
        })),
        total: Number(n), page, pageSize
      })
    }
    const formulaColorMatch = path.match(/^\/api\/formula\/colors\/(\d+)$/)
    if (req.method === 'GET' && formulaColorMatch) {
      const id = formulaColorMatch[1]
      const row = fq(`SELECT cc.*, b.name AS brand_name, m.model AS model_name
          FROM car_color cc
          LEFT JOIN car_brand b ON b.id = cc.brand_id
          LEFT JOIN car_model m ON m.id = cc.model_id
          WHERE cc.id = ? AND cc.is_deleted = '0'`).get(id)
      if (!row) return send(res, 404, { error: 'color not found' })
      // Colour numbers hang off the relation table (verified against the
      // imported data: relation.car_color = car_color.id and
      // relation.color_number = color_number.id — see import report).
      const numbers = fq(`SELECT cn.* FROM color_number_relation r
          JOIN color_number cn ON cn.id = r.color_number
          WHERE r.car_color = ? AND r.enable = '1' AND cn.is_deleted = '0'
          ORDER BY cn.id`).all(id)
      const lineStmt = fq("SELECT id, model, name, dose, color_rank FROM color_formula WHERE colornum_id = ? AND is_deleted = '0' ORDER BY color_rank, id")
      return send(res, 200, {
        id: String(row.id),
        brandId: String(row.brand_id), brandName: row.brand_name || '',
        modelId: String(row.model_id), modelName: row.model_name || '',
        carColor: row.car_color || '', factoryColor: row.factory_color || '',
        colorNum: row.color_num || '', cardNum: row.card_num || '',
        carImage: row.car_image || '', updateTime: row.update_time || '',
        numbers: numbers.map((cn) => ({
          id: String(cn.id), name: cn.name || '', colorName: cn.color_name || '',
          labL: numOrStr(cn.lab_l), labA: numOrStr(cn.lab_a), labB: numOrStr(cn.lab_b),
          formula: lineStmt.all(String(cn.id)).map((l) => ({
            id: String(l.id), model: l.model || '', name: l.name || '',
            dose: numOrStr(l.dose), colorRank: numOrStr(l.color_rank)
          }))
        }))
      })
    }

    // ---- Admin: paint formula library CRUD (perm: formula) ----
    const formulaCrudMatch = path.match(/^\/api\/admin\/formula\/([a-z-]+)(?:\/(\d+))?$/)
    if (formulaCrudMatch && FORMULA_CRUD[formulaCrudMatch[1]]) {
      const cfg = FORMULA_CRUD[formulaCrudMatch[1]]
      const id = formulaCrudMatch[2]
      if (req.method === 'GET' && !id) return send(res, 200, formulaList(cfg, url))
      if (req.method === 'POST' && !id) {
        const { values, error } = formulaValues(cfg, await readBody(req), true)
        if (error) return send(res, 400, { error })
        const newId = formulaInsert(cfg, values)
        return send(res, 201, formulaRowOut(formulaGet(cfg, newId)))
      }
      if (req.method === 'PUT' && id) {
        if (!formulaGet(cfg, id)) return send(res, 404, { error: 'record not found' })
        const { values, error } = formulaValues(cfg, await readBody(req), false)
        if (error) return send(res, 400, { error })
        formulaUpdate(cfg, id, values)
        return send(res, 200, formulaRowOut(formulaGet(cfg, id)))
      }
      if (req.method === 'DELETE' && id) {
        // Physical delete, same as the catalogue modules (products /
        // categories / brands all hard-delete from the admin console).
        fq(`DELETE FROM ${cfg.table} WHERE id = ?`).run(id)
        return send(res, 200, { ok: true })
      }
    }
    if (path === '/api/admin/formula-numbers' && req.method === 'GET') {
      return send(res, 200, formulaList(FORMULA_NUMBER_CFG, url))
    }
    if (path === '/api/admin/formula-numbers' && req.method === 'POST') {
      const { values, error } = formulaValues(FORMULA_NUMBER_CFG, await readBody(req), true)
      if (error) return send(res, 400, { error })
      const newId = formulaInsert(FORMULA_NUMBER_CFG, values)
      return send(res, 201, formulaRowOut(formulaGet(FORMULA_NUMBER_CFG, newId)))
    }
    const formulaNumberMatch = path.match(/^\/api\/admin\/formula-numbers\/(\d+)$/)
    if (formulaNumberMatch && req.method === 'PUT') {
      const id = formulaNumberMatch[1]
      if (!formulaGet(FORMULA_NUMBER_CFG, id)) return send(res, 404, { error: 'record not found' })
      const { values, error } = formulaValues(FORMULA_NUMBER_CFG, await readBody(req), false)
      if (error) return send(res, 400, { error })
      formulaUpdate(FORMULA_NUMBER_CFG, id, values)
      return send(res, 200, formulaRowOut(formulaGet(FORMULA_NUMBER_CFG, id)))
    }
    if (formulaNumberMatch && req.method === 'DELETE') {
      fq('DELETE FROM color_number WHERE id = ?').run(formulaNumberMatch[1])
      return send(res, 200, { ok: true })
    }
    // A colour number's formula lines: read them, or replace the whole
    // set in one PUT (the admin editor works on the full list at once).
    const formulaLinesMatch = path.match(/^\/api\/admin\/formula-numbers\/(\d+)\/lines$/)
    if (formulaLinesMatch && req.method === 'GET') {
      const id = formulaLinesMatch[1]
      if (!formulaGet(FORMULA_NUMBER_CFG, id)) return send(res, 404, { error: 'record not found' })
      const rows = fq("SELECT id, model, name, dose, color_rank FROM color_formula WHERE colornum_id = ? AND is_deleted = '0' ORDER BY color_rank, id").all(id)
      return send(res, 200, {
        colorNumberId: id,
        lines: rows.map((l) => ({
          id: String(l.id), model: l.model || '', name: l.name || '',
          dose: numOrStr(l.dose), colorRank: numOrStr(l.color_rank)
        }))
      })
    }
    if (formulaLinesMatch && req.method === 'PUT') {
      const id = formulaLinesMatch[1]
      if (!formulaGet(FORMULA_NUMBER_CFG, id)) return send(res, 404, { error: 'record not found' })
      const body = await readBody(req)
      if (!Array.isArray(body.lines)) return send(res, 400, { error: 'lines must be an array' })
      const stamp = nowStamp()
      db.exec('BEGIN')
      try {
        fq('DELETE FROM color_formula WHERE colornum_id = ?').run(id)
        const ins = fq(`INSERT INTO color_formula
          (colornum_id, name, model, dose, color_rank, enable, is_deleted, create_time, update_time)
          VALUES (?, ?, ?, ?, ?, '1', '0', ?, ?)`)
        body.lines.forEach((ln, i) => {
          ins.run(id, String(ln.name || ''), String(ln.model || ''),
            Number(ln.dose) || 0, Math.trunc(Number(ln.colorRank ?? ln.rank) || (i + 1)), stamp, stamp)
        })
        db.exec('COMMIT')
      } catch (e) {
        db.exec('ROLLBACK')
        throw e
      }
      const rows = fq("SELECT id, model, name, dose, color_rank FROM color_formula WHERE colornum_id = ? AND is_deleted = '0' ORDER BY color_rank, id").all(id)
      return send(res, 200, {
        colorNumberId: id,
        lines: rows.map((l) => ({
          id: String(l.id), model: l.model || '', name: l.name || '',
          dose: numOrStr(l.dose), colorRank: numOrStr(l.color_rank)
        }))
      })
    }

    // ---- Admin UI + uploaded images (static files; the UI asks for the token) ----
    if (req.method === 'GET' && (path === '/admin' || path.startsWith('/admin/'))) {
      const rel = path === '/admin' ? 'index.html' : path.slice('/admin/'.length)
      const file = join(ADMIN_DIR, rel)
      if (!file.startsWith(ADMIN_DIR)) return send(res, 404, { error: 'not found' })
      return sendFile(res, file)
    }
    if (req.method === 'GET' && path.startsWith('/uploads/')) {
      return sendFile(res, join(UPLOAD_DIR, basename(path)))
    }

    // ---- Admin: product management ----
    if (path === '/api/admin/products' && req.method === 'GET') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const rows = db.prepare('SELECT * FROM products ORDER BY sort, id').all()
      return send(res, 200, rows.map(adminProductOut))
    }
    if (path === '/api/admin/products' && req.method === 'POST') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const v = productValues(await readBody(req))
      if (typeof v === 'string') return send(res, 400, { error: v })
      const info = db.prepare(`INSERT INTO products
        (source_id, category_id, brand_id, names, price, ot_price, sales, unit, moq, image, video, tag, blurb, specs, sort, active)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`)
        .run(v.sourceId, v.categoryId, v.brandId, v.names, v.price, v.otPrice, v.sales, v.unit, v.moq, v.image, v.video, v.tag, v.blurb, v.specs, v.sort, v.active)
      const row = db.prepare('SELECT * FROM products WHERE id = ?').get(Number(info.lastInsertRowid))
      return send(res, 201, adminProductOut(row))
    }
    const adminProductMatch = path.match(/^\/api\/admin\/products\/(\d+)$/)
    if (adminProductMatch && req.method === 'PUT') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const id = Number(adminProductMatch[1])
      if (!db.prepare('SELECT id FROM products WHERE id = ?').get(id)) return send(res, 404, { error: 'product not found' })
      const v = productValues(await readBody(req))
      if (typeof v === 'string') return send(res, 400, { error: v })
      db.prepare(`UPDATE products SET source_id = ?, category_id = ?, brand_id = ?, names = ?, price = ?, ot_price = ?,
        sales = ?, unit = ?, moq = ?, image = ?, video = ?, tag = ?, blurb = ?, specs = ?, sort = ?, active = ? WHERE id = ?`)
        .run(v.sourceId, v.categoryId, v.brandId, v.names, v.price, v.otPrice, v.sales, v.unit, v.moq, v.image, v.video, v.tag, v.blurb, v.specs, v.sort, v.active, id)
      return send(res, 200, adminProductOut(db.prepare('SELECT * FROM products WHERE id = ?').get(id)))
    }
    if (adminProductMatch && req.method === 'DELETE') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      db.prepare('DELETE FROM products WHERE id = ?').run(Number(adminProductMatch[1]))
      return send(res, 200, { ok: true })
    }

    // ---- Admin: category management ----
    if (path === '/api/admin/categories' && req.method === 'GET') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const counts = db.prepare('SELECT category_id, COUNT(*) AS n FROM products GROUP BY category_id').all()
      const byId = Object.fromEntries(counts.map((r) => [r.category_id, r.n]))
      const rows = db.prepare('SELECT * FROM categories ORDER BY sort, id').all()
      return send(res, 200, rows.map((c) => ({ id: c.id, names: parseJsonCol(c.names, {}), sort: c.sort, count: byId[c.id] || 0 })))
    }
    if (path === '/api/admin/categories' && req.method === 'POST') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const body = await readBody(req)
      const names = body.names && typeof body.names === 'object' ? body.names : {}
      if (!String(names.en || names.zh || '').trim()) return send(res, 400, { error: 'category name (English) is required' })
      let id = String(body.id || '').trim()
      if (id && !/^[a-zA-Z0-9_-]+$/.test(id)) return send(res, 400, { error: 'category id may only contain letters, digits, - and _' })
      if (!id) id = 'c' + Date.now().toString(36)
      if (db.prepare('SELECT id FROM categories WHERE id = ?').get(id)) return send(res, 400, { error: 'category id already exists' })
      const sort = Math.trunc(Number(body.sort) || 0)
      db.prepare('INSERT INTO categories (id, names, sort) VALUES (?, ?, ?)').run(id, JSON.stringify(names), sort)
      return send(res, 201, { id, names, sort, count: 0 })
    }
    const adminCategoryMatch = path.match(/^\/api\/admin\/categories\/([^/]+)$/)
    if (adminCategoryMatch && req.method === 'PUT') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const id = decodeURIComponent(adminCategoryMatch[1])
      if (!db.prepare('SELECT id FROM categories WHERE id = ?').get(id)) return send(res, 404, { error: 'category not found' })
      const body = await readBody(req)
      const names = body.names && typeof body.names === 'object' ? body.names : {}
      if (!String(names.en || names.zh || '').trim()) return send(res, 400, { error: 'category name (English) is required' })
      const sort = Math.trunc(Number(body.sort) || 0)
      db.prepare('UPDATE categories SET names = ?, sort = ? WHERE id = ?').run(JSON.stringify(names), sort, id)
      const { n } = db.prepare('SELECT COUNT(*) AS n FROM products WHERE category_id = ?').get(id)
      return send(res, 200, { id, names, sort, count: n })
    }
    if (adminCategoryMatch && req.method === 'DELETE') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const id = decodeURIComponent(adminCategoryMatch[1])
      const { n } = db.prepare('SELECT COUNT(*) AS n FROM products WHERE category_id = ?').get(id)
      if (n > 0) return send(res, 400, { error: `category still has ${n} product(s); move or delete them first` })
      db.prepare('DELETE FROM categories WHERE id = ?').run(id)
      return send(res, 200, { ok: true })
    }

    // ---- Admin: brand management (mirrors plat's brand module) ----
    if (path === '/api/admin/brands' && req.method === 'GET') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const rows = db.prepare('SELECT * FROM brands ORDER BY sort, id').all()
      return send(res, 200, rows.map(adminBrandOut))
    }
    if (path === '/api/admin/brands' && req.method === 'POST') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const body = await readBody(req)
      const names = body.names && typeof body.names === 'object' ? body.names : {}
      if (!String(names.en || names.zh || '').trim()) return send(res, 400, { error: 'brand name (English) is required' })
      let id = String(body.id || '').trim()
      if (id && !/^[a-zA-Z0-9_-]+$/.test(id)) return send(res, 400, { error: 'brand id may only contain letters, digits, - and _' })
      if (!id) id = 'b' + Date.now().toString(36)
      if (db.prepare('SELECT id FROM brands WHERE id = ?').get(id)) return send(res, 400, { error: 'brand id already exists' })
      db.prepare('INSERT INTO brands (id, names, logo, sort, active) VALUES (?, ?, ?, ?, ?)')
        .run(id, JSON.stringify(names), String(body.logo || ''), Math.trunc(Number(body.sort) || 0), body.active === false ? 0 : 1)
      return send(res, 201, adminBrandOut(db.prepare('SELECT * FROM brands WHERE id = ?').get(id)))
    }
    const adminBrandMatch = path.match(/^\/api\/admin\/brands\/([^/]+)$/)
    if (adminBrandMatch && req.method === 'PUT') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const id = decodeURIComponent(adminBrandMatch[1])
      if (!db.prepare('SELECT id FROM brands WHERE id = ?').get(id)) return send(res, 404, { error: 'brand not found' })
      const body = await readBody(req)
      const names = body.names && typeof body.names === 'object' ? body.names : {}
      if (!String(names.en || names.zh || '').trim()) return send(res, 400, { error: 'brand name (English) is required' })
      db.prepare('UPDATE brands SET names = ?, logo = ?, sort = ?, active = ? WHERE id = ?')
        .run(JSON.stringify(names), String(body.logo || ''), Math.trunc(Number(body.sort) || 0), body.active === false ? 0 : 1, id)
      return send(res, 200, adminBrandOut(db.prepare('SELECT * FROM brands WHERE id = ?').get(id)))
    }
    if (adminBrandMatch && req.method === 'DELETE') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const id = decodeURIComponent(adminBrandMatch[1])
      const { n } = db.prepare('SELECT COUNT(*) AS n FROM products WHERE brand_id = ?').get(id)
      if (n > 0) return send(res, 400, { error: `brand still has ${n} product(s); reassign or delete them first` })
      db.prepare('DELETE FROM brands WHERE id = ?').run(id)
      return send(res, 200, { ok: true })
    }

    // Public brand list for the storefront (only brands that are switched on).
    if (req.method === 'GET' && path === '/api/brands') {
      const rows = db.prepare('SELECT * FROM brands WHERE active = 1 ORDER BY sort, id').all()
      return send(res, 200, rows.map((b) => ({ id: b.id, name: pickLang(b.names, lang), logo: b.logo || '' })))
    }

    // ---- Admin: image / video upload (base64 JSON keeps the server dependency-free) ----
    // Images keep the original rules (png/jpg/webp/gif, <= 6 MB decoded).
    // Videos (mp4/webm/mov) are allowed up to 50 MB decoded; base64 inflates
    // that to ~68 MB of JSON, so ONLY this route's body limit is raised to
    // 80 MB — every other route keeps the small default readBody limit.
    if (path === '/api/admin/upload' && req.method === 'POST') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const body = await readBody(req, 80e6)
      const ext = extname(String(body.filename || '')).toLowerCase()
      const isVideo = ['.mp4', '.webm', '.mov'].includes(ext)
      if (!isVideo && !['.png', '.jpg', '.jpeg', '.webp', '.gif'].includes(ext)) {
        return send(res, 400, { error: 'only png/jpg/webp/gif images or mp4/webm/mov videos are allowed' })
      }
      const buf = Buffer.from(String(body.dataBase64 || ''), 'base64')
      if (isVideo) {
        if (buf.length === 0 || buf.length > 50e6) return send(res, 400, { error: 'video is empty or larger than 50 MB' })
      } else if (buf.length === 0 || buf.length > 6e6) {
        return send(res, 400, { error: 'image is empty or larger than 6 MB' })
      }
      const safe = basename(String(body.filename)).replace(/[^a-zA-Z0-9._-]/g, '-')
      const name = `${Date.now()}-${safe}`
      writeFileSync(join(UPLOAD_DIR, name), buf)
      const proto = req.headers['x-forwarded-proto'] || 'http'
      return send(res, 201, { url: `${proto}://${req.headers.host}/uploads/${name}` })
    }

    // ---- Admin: login / session / current account ----
    if (path === '/api/admin/login' && req.method === 'POST') {
      const body = await readBody(req)
      const user = db.prepare('SELECT * FROM admin_users WHERE username = ?').get(String(body.username || '').trim())
      if (!user || user.active !== 1 || !verifyPassword(body.password || '', user.password_hash)) {
        return send(res, 401, { error: 'username or password incorrect' })
      }
      const token = randomBytes(32).toString('hex')
      db.prepare('INSERT INTO admin_sessions (token, user_id, expires_at) VALUES (?, ?, ?)').run(token, user.id, Date.now() + SESSION_TTL)
      const role = db.prepare('SELECT * FROM admin_roles WHERE id = ?').get(user.role_id)
      return send(res, 200, {
        token,
        user: {
          username: user.username, displayName: user.display_name,
          roleName: role ? role.name : '', permissions: role ? parseJsonCol(role.permissions, []) : []
        }
      })
    }
    if (path === '/api/admin/logout' && req.method === 'POST') {
      db.prepare('DELETE FROM admin_sessions WHERE token = ?').run(req.headers['x-admin-token'] || '')
      return send(res, 200, { ok: true })
    }
    if (path === '/api/admin/me' && req.method === 'GET') {
      const a = req.adminAuth
      return send(res, 200, { username: a.username, displayName: a.displayName, roleName: a.roleName, permissions: a.permissions })
    }
    if (path === '/api/admin/permissions' && req.method === 'GET') {
      return send(res, 200, PERMISSIONS)
    }

    // ---- Admin: user accounts ----
    if (path === '/api/admin/users' && req.method === 'GET') {
      const rows = db.prepare('SELECT * FROM admin_users ORDER BY id').all()
      return send(res, 200, rows.map(adminUserOut))
    }
    if (path === '/api/admin/users' && req.method === 'POST') {
      const body = await readBody(req)
      const username = String(body.username || '').trim()
      const password = String(body.password || '')
      if (!/^[a-zA-Z0-9_.-]{2,32}$/.test(username)) return send(res, 400, { error: 'username must be 2-32 chars (letters, digits, _ . -)' })
      if (password.length < 6) return send(res, 400, { error: 'password must be at least 6 characters' })
      if (db.prepare('SELECT id FROM admin_users WHERE username = ?').get(username)) return send(res, 400, { error: 'username already exists' })
      const roleId = Number(body.roleId) || 0
      if (!db.prepare('SELECT id FROM admin_roles WHERE id = ?').get(roleId)) return send(res, 400, { error: 'role not found' })
      const info = db.prepare('INSERT INTO admin_users (username, password_hash, display_name, role_id, active, created_at) VALUES (?, ?, ?, ?, ?, ?)')
        .run(username, hashPassword(password), String(body.displayName || username), roleId, body.active === false ? 0 : 1, new Date().toISOString())
      return send(res, 201, adminUserOut(db.prepare('SELECT * FROM admin_users WHERE id = ?').get(Number(info.lastInsertRowid))))
    }
    const adminUserMatch = path.match(/^\/api\/admin\/users\/(\d+)$/)
    if (adminUserMatch && req.method === 'PUT') {
      const id = Number(adminUserMatch[1])
      const user = db.prepare('SELECT * FROM admin_users WHERE id = ?').get(id)
      if (!user) return send(res, 404, { error: 'user not found' })
      const body = await readBody(req)
      const roleId = body.roleId === undefined ? user.role_id : Number(body.roleId)
      if (!db.prepare('SELECT id FROM admin_roles WHERE id = ?').get(roleId)) return send(res, 400, { error: 'role not found' })
      const active = body.active === undefined ? user.active : (body.active ? 1 : 0)
      if (user.active === 1 && isSuperRole(user.role_id) && (active === 0 || !isSuperRole(roleId)) && !otherSuperExists(id)) {
        return send(res, 400, { error: 'keep at least one active super admin' })
      }
      let hash = user.password_hash
      if (body.password) {
        if (String(body.password).length < 6) return send(res, 400, { error: 'password must be at least 6 characters' })
        hash = hashPassword(body.password)
      }
      db.prepare('UPDATE admin_users SET display_name = ?, role_id = ?, active = ?, password_hash = ? WHERE id = ?')
        .run(body.displayName === undefined ? user.display_name : String(body.displayName), roleId, active, hash, id)
      if (active === 0) db.prepare('DELETE FROM admin_sessions WHERE user_id = ?').run(id)
      return send(res, 200, adminUserOut(db.prepare('SELECT * FROM admin_users WHERE id = ?').get(id)))
    }
    if (adminUserMatch && req.method === 'DELETE') {
      const id = Number(adminUserMatch[1])
      if (req.adminAuth.userId === id) return send(res, 400, { error: 'cannot delete your own account' })
      const user = db.prepare('SELECT * FROM admin_users WHERE id = ?').get(id)
      if (!user) return send(res, 404, { error: 'user not found' })
      if (user.active === 1 && isSuperRole(user.role_id) && !otherSuperExists(id)) {
        return send(res, 400, { error: 'keep at least one active super admin' })
      }
      db.prepare('DELETE FROM admin_sessions WHERE user_id = ?').run(id)
      db.prepare('DELETE FROM admin_users WHERE id = ?').run(id)
      return send(res, 200, { ok: true })
    }

    // ---- Admin: roles (a role = a set of menu permissions) ----
    if (path === '/api/admin/roles' && req.method === 'GET') {
      const rows = db.prepare('SELECT * FROM admin_roles ORDER BY id').all()
      return send(res, 200, rows.map(adminRoleOut))
    }
    if (path === '/api/admin/roles' && req.method === 'POST') {
      const body = await readBody(req)
      const name = String(body.name || '').trim()
      if (!name) return send(res, 400, { error: 'role name is required' })
      if (db.prepare('SELECT id FROM admin_roles WHERE name = ?').get(name)) return send(res, 400, { error: 'role name already exists' })
      const perms = Array.isArray(body.permissions) ? body.permissions.filter((p) => ALL_PERMS.includes(p) || p === '*') : []
      const info = db.prepare('INSERT INTO admin_roles (name, permissions, builtin) VALUES (?, ?, 0)').run(name, JSON.stringify(perms))
      return send(res, 201, adminRoleOut(db.prepare('SELECT * FROM admin_roles WHERE id = ?').get(Number(info.lastInsertRowid))))
    }
    const adminRoleMatch = path.match(/^\/api\/admin\/roles\/(\d+)$/)
    if (adminRoleMatch && req.method === 'PUT') {
      const id = Number(adminRoleMatch[1])
      const role = db.prepare('SELECT * FROM admin_roles WHERE id = ?').get(id)
      if (!role) return send(res, 404, { error: 'role not found' })
      const body = await readBody(req)
      const name = body.name === undefined ? role.name : String(body.name).trim()
      if (!name) return send(res, 400, { error: 'role name is required' })
      const dup = db.prepare('SELECT id FROM admin_roles WHERE name = ? AND id != ?').get(name, id)
      if (dup) return send(res, 400, { error: 'role name already exists' })
      // The built-in super-admin role always keeps full permissions.
      const perms = role.builtin === 1
        ? ['*']
        : (Array.isArray(body.permissions) ? body.permissions.filter((p) => ALL_PERMS.includes(p) || p === '*') : parseJsonCol(role.permissions, []))
      db.prepare('UPDATE admin_roles SET name = ?, permissions = ? WHERE id = ?').run(name, JSON.stringify(perms), id)
      return send(res, 200, adminRoleOut(db.prepare('SELECT * FROM admin_roles WHERE id = ?').get(id)))
    }
    if (adminRoleMatch && req.method === 'DELETE') {
      const id = Number(adminRoleMatch[1])
      const role = db.prepare('SELECT * FROM admin_roles WHERE id = ?').get(id)
      if (!role) return send(res, 404, { error: 'role not found' })
      if (role.builtin === 1) return send(res, 400, { error: 'built-in role cannot be deleted' })
      const { n } = db.prepare('SELECT COUNT(*) AS n FROM admin_users WHERE role_id = ?').get(id)
      if (n > 0) return send(res, 400, { error: `role still has ${n} user(s); reassign them first` })
      db.prepare('DELETE FROM admin_roles WHERE id = ?').run(id)
      return send(res, 200, { ok: true })
    }

    return send(res, 404, { error: 'not found' })
  } catch (err) {
    return send(res, 500, { error: String(err && err.message || err) })
  }
})

server.listen(PORT, () => {
  console.log(`taojie-overseas-server listening on http://localhost:${PORT} (db: ${DB_PATH})`)
  if (!ADMIN_TOKEN) console.log('warning: ADMIN_TOKEN not set — root token disabled; set it once to bootstrap the first admin account (admin / ADMIN_TOKEN)')
})
