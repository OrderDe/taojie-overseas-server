// Taojie overseas mall — lightweight backend.
// Zero npm dependencies: Node >= 22 (uses node:http + node:sqlite).
//
// Serves the public catalogue API the overseas PC showcase expects:
//   GET  /api/health
//   GET  /api/categories?lang=en
//   GET  /api/products?lang=&keyword=&categoryId=&page=1&limit=48
//   GET  /api/products/:id?lang=
//   GET  /api/keywords
//   GET  /api/site-config
//   POST /api/rfq                      (lead capture, stored in SQLite)
//   GET  /api/admin/rfq                (header: x-admin-token)
//   PUT  /api/admin/site-config        (header: x-admin-token)
//   GET|POST|PUT|DELETE /api/admin/products[/:id]     (catalogue management)
//   GET|POST|PUT|DELETE /api/admin/categories[/:id]   (category management)
//   POST /api/admin/upload             (JSON {filename, dataBase64} -> {url})
//   GET  /admin                        (admin UI, static files in admin/)
//   GET  /uploads/<file>               (uploaded product images)
//
// Data lives in a single SQLite file (data/app.db). On first boot the
// bundled seed (data/seed.json) is imported; replace/extend it with real
// products via scripts/import-products.mjs (tjmall export -> upsert).

import { createServer } from 'node:http'
import { DatabaseSync } from 'node:sqlite'
import { readFileSync, existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join, extname, basename } from 'node:path'
import { fileURLToPath } from 'node:url'

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
`)
// Products gained a sort column after the first release; add it in place.
try { db.exec('ALTER TABLE products ADD COLUMN sort INTEGER NOT NULL DEFAULT 0') } catch { /* already there */ }
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
    (id, source_id, category_id, names, price, ot_price, sales, unit, moq, image, tag, blurb, specs, active)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)`)
  for (const p of seed.products || []) {
    insP.run(p.id, String(p.sourceId ?? p.id), String(p.categoryId || ''),
      JSON.stringify(p.names || { en: p.name || '' }), Number(p.price) || 0, Number(p.otPrice) || 0,
      Number(p.sales) || 0, String(p.unit || 'pc'), Number(p.moq) || 0, String(p.image || ''),
      String(p.tag || ''), String(p.blurb || ''), JSON.stringify(p.specs || {}))
  }
  console.log(`seeded ${seed.products?.length || 0} products, ${seed.categories?.length || 0} categories`)
}
seedIfEmpty()

const DEFAULT_CONFIG = {
  defaultTheme: 'h', defaultLanguage: 'en', heroTitle: '', heroSubtitle: '',
  announcement: '', showRfq: true, whatsapp: '', email: '', currency: 'USD'
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

function productOut(row, lang) {
  const cat = db.prepare('SELECT names FROM categories WHERE id = ?').get(row.category_id)
  let specs = {}
  try { specs = JSON.parse(row.specs || '{}') } catch { /* keep empty */ }
  return {
    id: row.id,
    name: pickLang(row.names, lang),
    categoryId: row.category_id,
    category: cat ? pickLang(cat.names, lang) : '',
    price: row.price,
    otPrice: row.ot_price,
    sales: row.sales,
    unit: row.unit,
    moq: row.moq,
    image: row.image,
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

function isAdmin(req) {
  return ADMIN_TOKEN !== '' && req.headers['x-admin-token'] === ADMIN_TOKEN
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

// ---- Admin: catalogue management + static files (admin UI, uploads) ----

const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json; charset=utf-8',
  '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg',
  '.webp': 'image/webp', '.gif': 'image/gif', '.svg': 'image/svg+xml', '.ico': 'image/x-icon'
}

function sendFile(res, filePath) {
  if (!existsSync(filePath)) return send(res, 404, { error: 'not found' })
  res.writeHead(200, { 'Content-Type': MIME[extname(filePath).toLowerCase()] || 'application/octet-stream' })
  res.end(readFileSync(filePath))
}

function parseJsonCol(text, fallback) {
  try { const v = JSON.parse(text || ''); return v && typeof v === 'object' ? v : fallback } catch { return fallback }
}

function adminProductOut(row) {
  return {
    id: row.id,
    sourceId: row.source_id,
    categoryId: row.category_id,
    names: parseJsonCol(row.names, {}),
    price: row.price,
    otPrice: row.ot_price,
    sales: row.sales,
    unit: row.unit,
    moq: row.moq,
    image: row.image,
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
  return {
    sourceId: String(body.sourceId ?? ''),
    categoryId: String(body.categoryId || ''),
    names: JSON.stringify(names),
    price: Number(body.price) || 0,
    otPrice: Number(body.otPrice) || 0,
    sales: Math.max(0, Math.trunc(Number(body.sales) || 0)),
    unit: String(body.unit || 'pc'),
    moq: Math.max(0, Math.trunc(Number(body.moq) || 0)),
    image: String(body.image || ''),
    tag: String(body.tag || ''),
    blurb: String(body.blurb || ''),
    specs: JSON.stringify(body.specs && typeof body.specs === 'object' ? body.specs : {}),
    sort: Math.trunc(Number(body.sort) || 0),
    active: body.active === false ? 0 : 1
  }
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost')
  const path = url.pathname.replace(/\/$/, '') || '/'
  const lang = LANGS.includes(url.searchParams.get('lang')) ? url.searchParams.get('lang') : 'en'
  try {
    if (req.method === 'OPTIONS') return send(res, 204, {})

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
      const page = Math.max(1, Number(url.searchParams.get('page') || 1))
      const limit = Math.min(100, Math.max(1, Number(url.searchParams.get('limit') || 48)))
      let rows = db.prepare('SELECT * FROM products WHERE active = 1 ORDER BY sort, id').all()
      if (categoryId) rows = rows.filter((r) => r.category_id === categoryId)
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

    if (req.method === 'PUT' && path === '/api/admin/site-config') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const body = await readBody(req)
      const merged = { ...getConfig() }
      for (const k of Object.keys(DEFAULT_CONFIG)) if (body[k] !== undefined) merged[k] = body[k]
      db.prepare('INSERT INTO site_config (id, data) VALUES (1, ?) ON CONFLICT(id) DO UPDATE SET data = excluded.data')
        .run(JSON.stringify(merged))
      return send(res, 200, merged)
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
        (source_id, category_id, names, price, ot_price, sales, unit, moq, image, tag, blurb, specs, sort, active)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`)
        .run(v.sourceId, v.categoryId, v.names, v.price, v.otPrice, v.sales, v.unit, v.moq, v.image, v.tag, v.blurb, v.specs, v.sort, v.active)
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
      db.prepare(`UPDATE products SET source_id = ?, category_id = ?, names = ?, price = ?, ot_price = ?,
        sales = ?, unit = ?, moq = ?, image = ?, tag = ?, blurb = ?, specs = ?, sort = ?, active = ? WHERE id = ?`)
        .run(v.sourceId, v.categoryId, v.names, v.price, v.otPrice, v.sales, v.unit, v.moq, v.image, v.tag, v.blurb, v.specs, v.sort, v.active, id)
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

    // ---- Admin: image upload (base64 JSON keeps the server dependency-free) ----
    if (path === '/api/admin/upload' && req.method === 'POST') {
      if (!isAdmin(req)) return send(res, 401, { error: 'admin token required' })
      const body = await readBody(req, 12e6)
      const ext = extname(String(body.filename || '')).toLowerCase()
      if (!['.png', '.jpg', '.jpeg', '.webp', '.gif'].includes(ext)) return send(res, 400, { error: 'only png/jpg/webp/gif images are allowed' })
      const buf = Buffer.from(String(body.dataBase64 || ''), 'base64')
      if (buf.length === 0 || buf.length > 6e6) return send(res, 400, { error: 'image is empty or larger than 6 MB' })
      const safe = basename(String(body.filename)).replace(/[^a-zA-Z0-9._-]/g, '-')
      const name = `${Date.now()}-${safe}`
      writeFileSync(join(UPLOAD_DIR, name), buf)
      const proto = req.headers['x-forwarded-proto'] || 'http'
      return send(res, 201, { url: `${proto}://${req.headers.host}/uploads/${name}` })
    }

    return send(res, 404, { error: 'not found' })
  } catch (err) {
    return send(res, 500, { error: String(err && err.message || err) })
  }
})

server.listen(PORT, () => {
  console.log(`taojie-overseas-server listening on http://localhost:${PORT} (db: ${DB_PATH})`)
  if (!ADMIN_TOKEN) console.log('warning: ADMIN_TOKEN not set — admin endpoints are disabled')
})
