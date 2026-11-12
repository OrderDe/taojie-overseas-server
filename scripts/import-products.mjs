// Import / refresh products from a JSON export (e.g. produced from the
// tjmall database) into the overseas catalogue. Upserts by product id.
//
// Usage:  node scripts/import-products.mjs <file.json>
//
// Expected JSON shape (same as data/seed.json):
// {
//   "categories": [{ "id": "paint", "names": { "en": "Automotive Paint", "zh": "汽车漆" } }],
//   "products":   [{ "id": 1, "sourceId": "tjmall-10001", "categoryId": "paint",
//                    "names": { "en": "...", "zh": "..." }, "price": 9.9, "otPrice": 14.5,
//                    "sales": 0, "unit": "1L tin", "moq": 100, "image": "https://...",
//                    "video": "", "tag": "", "blurb": "...", "specs": { "Mix ratio": "2 : 1" } }]
// }
import { DatabaseSync } from 'node:sqlite'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const file = process.argv[2]
if (!file) { console.error('usage: node scripts/import-products.mjs <file.json>'); process.exit(1) }
const data = JSON.parse(readFileSync(file, 'utf8'))
const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const db = new DatabaseSync(process.env.DB_PATH || join(ROOT, 'data', 'app.db'))

const insC = db.prepare('INSERT INTO categories (id, names, sort) VALUES (?, ?, ?) ON CONFLICT(id) DO UPDATE SET names = excluded.names')
;(data.categories || []).forEach((c, i) => insC.run(String(c.id), JSON.stringify(c.names || { en: c.name || '' }), i))

const insP = db.prepare(`INSERT INTO products
  (id, source_id, category_id, names, price, ot_price, sales, unit, moq, image, video, tag, blurb, specs, active)
  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
  ON CONFLICT(id) DO UPDATE SET source_id = excluded.source_id, category_id = excluded.category_id,
    names = excluded.names, price = excluded.price, ot_price = excluded.ot_price, sales = excluded.sales,
    unit = excluded.unit, moq = excluded.moq, image = excluded.image, video = excluded.video, tag = excluded.tag,
    blurb = excluded.blurb, specs = excluded.specs, active = 1`)
let n = 0
for (const p of data.products || []) {
  insP.run(p.id, String(p.sourceId ?? p.id), String(p.categoryId || ''),
    JSON.stringify(p.names || { en: p.name || '' }), Number(p.price) || 0, Number(p.otPrice) || 0,
    Number(p.sales) || 0, String(p.unit || 'pc'), Number(p.moq) || 0, String(p.image || ''),
    String(p.video || ''), String(p.tag || ''), String(p.blurb || ''), JSON.stringify(p.specs || {}))
  n++
}
console.log(`imported ${n} products, ${(data.categories || []).length} categories from ${file}`)
