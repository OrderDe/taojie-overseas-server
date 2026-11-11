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
//
// Data lives in a single SQLite file (data/app.db). On first boot the
// bundled seed (data/seed.json) is imported; replace/extend it with real
// products via scripts/import-products.mjs (tjmall export -> upsert).

import { createServer } from 'node:http'
import { DatabaseSync } from 'node:sqlite'
import { readFileSync, existsSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const PORT = Number(process.env.PORT || 3100)
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || ''
const DB_PATH = process.env.DB_PATH || join(ROOT, 'data', 'app.db')
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
    'Access-Control-Allow-Methods': 'GET, POST, PUT, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, x-admin-token'
  })
  res.end(text)
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = ''
    req.on('data', (c) => { data += c; if (data.length > 1e6) req.destroy() })
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
      let rows = db.prepare('SELECT * FROM products WHERE active = 1 ORDER BY id').all()
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

    return send(res, 404, { error: 'not found' })
  } catch (err) {
    return send(res, 500, { error: String(err && err.message || err) })
  }
})

server.listen(PORT, () => {
  console.log(`taojie-overseas-server listening on http://localhost:${PORT} (db: ${DB_PATH})`)
  if (!ADMIN_TOKEN) console.log('warning: ADMIN_TOKEN not set — admin endpoints are disabled')
})
