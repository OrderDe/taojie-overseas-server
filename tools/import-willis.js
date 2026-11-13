// Import the williscarpaint.com product catalogue into this backend's
// SQLite DB, as real brand / category / product rows the admin console
// can manage and the storefront can navigate, filter and search.
//
//   node tools/import-willis.js [dataFile] [dbPath]
//
// Defaults: dataFile = data/willis_catalog.json (bundled with the repo),
// dbPath = $DB_PATH or data/app.db. The catalogue JSON is a scrape of the
// company's own public storefront (williscarpaint.com): 10 collections
// and every product in them, shaped as
//   { categories: [{slug, nameEn, nameZh}],
//     products:   [{slug, title, categorySlug, image, blurb,
//                   specs: {ordered key/value attributes}}] }
//
// Upsert semantics: the WILLIS brand and the 10 categories are matched
// by their English name (names.en), products by source_id
// 'willis:<slug>'; missing rows are inserted, present rows whose
// scraped fields changed are updated in place, identical rows are
// skipped — so re-running is safe and catalogue refreshes (e.g. the
// product spec tables) reach existing databases. All writes run in a
// single transaction. src/server.js runs this same upsert on startup
// whenever the bundled file's hash differs from the hash stored in
// the site config (key willisImportHash).
//
// Java/MySQL deployments: this tool only targets the Node/SQLite stack.
// For the Java twin, generate the equivalent INSERTs from the same
// data/willis_catalog.json (brand WILLIS, the 10 categories, products
// with negotiable = 1 and source_id 'willis:<slug>'); nothing in the
// Java schema needs to change for these rows.

import { DatabaseSync } from 'node:sqlite'
import { readFileSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const dataFile = process.argv[2] || join(ROOT, 'data', 'willis_catalog.json')
const dbPath = process.argv[3] || process.env.DB_PATH || join(ROOT, 'data', 'app.db')

// The mall tables this import writes to, mirroring src/server.js (the
// CREATEs plus the in-place column additions), so a standalone run
// against a fresh db path works too. Keep in sync with src/server.js.
const DDL = `
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
    negotiable INTEGER NOT NULL DEFAULT 0,
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
  CREATE TABLE IF NOT EXISTS brands (
    id TEXT PRIMARY KEY,
    names TEXT NOT NULL DEFAULT '{}',
    logo TEXT NOT NULL DEFAULT '',
    sort INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1
  );
`
const PRODUCT_ALTERS = [
  'ALTER TABLE products ADD COLUMN sort INTEGER NOT NULL DEFAULT 0',
  "ALTER TABLE products ADD COLUMN brand_id TEXT NOT NULL DEFAULT ''",
  "ALTER TABLE products ADD COLUMN video TEXT NOT NULL DEFAULT ''",
  'ALTER TABLE products ADD COLUMN negotiable INTEGER NOT NULL DEFAULT 0'
]

export function findBundledDataFile(root) {
  const f = join(root, 'data', 'willis_catalog.json')
  return existsSync(f) ? f : null
}

export function loadCatalog(file) {
  const catalog = JSON.parse(readFileSync(file, 'utf8'))
  if (!catalog || !Array.isArray(catalog.categories) || !Array.isArray(catalog.products)) {
    throw new Error(`${file}: expected { categories: [...], products: [...] }`)
  }
  return catalog
}

function parseNames(json) {
  try { return JSON.parse(json || '{}') } catch { return {} }
}

// Find a row in brands/categories whose names.en matches exactly — the
// stable dedupe key for entities that have no slug column.
function findByEnglishName(db, table, englishName) {
  for (const row of db.prepare(`SELECT id, names FROM ${table}`).all()) {
    if (parseNames(row.names).en === englishName) return row.id
  }
  return null
}

function freshId(db, table, base) {
  if (!db.prepare(`SELECT id FROM ${table} WHERE id = ?`).get(base)) return base
  let i = 2
  while (db.prepare(`SELECT id FROM ${table} WHERE id = ?`).get(`${base}-${i}`)) i++
  return `${base}-${i}`
}

// Insert the catalogue into an open DB whose mall tables already exist
// (the caller's DDL). Returns human-readable result lines with counts.
export function importIntoDb(db, catalog) {
  const results = []
  db.exec('BEGIN')
  try {
    // Brand: WILLIS (matched by English name).
    let brandId = findByEnglishName(db, 'brands', 'WILLIS')
    if (brandId) {
      results.push(`brand WILLIS: skipped (already exists, id=${brandId})`)
    } else {
      brandId = freshId(db, 'brands', 'willis')
      db.prepare('INSERT INTO brands (id, names, logo, sort, active) VALUES (?, ?, ?, ?, 1)')
        .run(brandId, JSON.stringify({ en: 'WILLIS', zh: '威利斯' }), '', 100)
      results.push(`brand WILLIS: inserted (id=${brandId})`)
    }

    // Categories: matched by English name; new ones get a stable
    // 'willis-<slug>' id and sort after the existing catalogue (100+).
    const categoryIdBySlug = {}
    let catInserted = 0, catSkipped = 0
    catalog.categories.forEach((c, i) => {
      const existing = findByEnglishName(db, 'categories', c.nameEn)
      if (existing) {
        categoryIdBySlug[c.slug] = existing
        catSkipped++
        return
      }
      const id = freshId(db, 'categories', `willis-${c.slug}`)
      db.prepare('INSERT INTO categories (id, names, sort) VALUES (?, ?, ?)')
        .run(id, JSON.stringify({ en: c.nameEn, zh: c.nameZh || '' }), 100 + i)
      categoryIdBySlug[c.slug] = id
      catInserted++
    })
    results.push(`categories: inserted ${catInserted}, skipped ${catSkipped} (already present)`)

    // Products: matched by source_id 'willis:<slug>'. Prices stay 0 and
    // negotiable = 1 (price on request), mirroring the source storefront
    // which shows no public prices. Existing rows are updated when the
    // scraped fields (names / category / brand / image / blurb / specs)
    // changed, so a refreshed catalogue enriches products already in
    // the database instead of being skipped wholesale.
    const insP = db.prepare(`INSERT INTO products
      (source_id, category_id, brand_id, names, price, ot_price, negotiable, sales, unit, moq, image, video, tag, blurb, specs, sort, active)
      VALUES (?, ?, ?, ?, 0, 0, 1, 0, '', 0, ?, '', '', ?, ?, 0, 1)`)
    const updP = db.prepare(`UPDATE products
      SET category_id = ?, brand_id = ?, names = ?, image = ?, blurb = ?, specs = ?
      WHERE id = ?`)
    const getP = db.prepare('SELECT id, category_id, brand_id, names, image, blurb, specs FROM products WHERE source_id = ?')
    let prodInserted = 0, prodUpdated = 0, prodSkipped = 0, prodNoCategory = 0
    for (const p of catalog.products) {
      const sourceId = `willis:${p.slug}`
      const categoryId = categoryIdBySlug[p.categorySlug] || ''
      if (!categoryId) prodNoCategory++
      const names = JSON.stringify({ en: p.title || p.slug })
      const image = String(p.image || '')
      const blurb = String(p.blurb || '')
      const specs = JSON.stringify(p.specs && typeof p.specs === 'object' ? p.specs : {})
      const existing = getP.get(sourceId)
      if (!existing) {
        insP.run(sourceId, categoryId, brandId, names, image, blurb, specs)
        prodInserted++
        continue
      }
      if (existing.category_id === categoryId && existing.brand_id === brandId &&
          existing.names === names && existing.image === image &&
          existing.blurb === blurb && existing.specs === specs) {
        prodSkipped++
        continue
      }
      updP.run(categoryId, brandId, names, image, blurb, specs, existing.id)
      prodUpdated++
    }
    results.push(`products: inserted ${prodInserted}, updated ${prodUpdated}, skipped ${prodSkipped} (unchanged)` +
      (prodNoCategory ? `, ${prodNoCategory} without a category` : ''))
    db.exec('COMMIT')
  } catch (e) {
    db.exec('ROLLBACK')
    throw e
  }
  return results
}

function main() {
  if (!existsSync(dataFile)) {
    console.error(`data file not found: ${dataFile}`)
    process.exit(1)
  }
  const catalog = loadCatalog(dataFile)
  const db = new DatabaseSync(dbPath)
  db.exec(DDL)
  for (const alter of PRODUCT_ALTERS) {
    try { db.exec(alter) } catch { /* already there */ }
  }
  const results = importIntoDb(db, catalog)
  console.log(`import into ${dbPath} complete:`)
  for (const line of results) console.log(`  ${line}`)
  db.close()
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main()
