// Import the sfsy paint-formula data dump into this backend's SQLite DB.
//
//   node tools/import-sfsy.js [dataFile] [dbPath]
//
// Defaults: dataFile = data/sfsy_data.sql.gz when present, else the
// bundled data/sfsy_data.sql.gz.b64 (base64 of the same gz — the repo
// stores it as text so snapshot pushes can carry it), else a plain
// data/sfsy_data.sql. dbPath = $DB_PATH or data/app.db. Input may be
// plain .sql, .sql.gz, or .sql.gz.b64. Only the 15 formula business tables are imported; a table
// that already holds rows is skipped, so re-running is safe (idempotent).
// All inserts run in a single transaction.
//
// The dump is mysqldump "extended insert" output — INSERT statements
// without column lists — so rows are bound positionally against the
// table definitions below, which mirror the source schema column order
// exactly. Keep this DDL in sync with src/server.js.

import { DatabaseSync } from 'node:sqlite'
import { readFileSync, existsSync } from 'node:fs'
import { gunzipSync } from 'node:zlib'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const bundled = ['sfsy_data.sql.gz', 'sfsy_data.sql.gz.b64', 'sfsy_data.sql']
  .map((n) => join(ROOT, 'data', n)).find((f) => existsSync(f))
const dataFile = process.argv[2] || bundled || join(ROOT, 'data', 'sfsy_data.sql.gz')
const dbPath = process.argv[3] || process.env.DB_PATH || join(ROOT, 'data', 'app.db')

const DDL = `
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
`

const TABLES = [
  'car_brand', 'car_model', 'car_country', 'car_factory', 'color_card',
  'color_masterbatch', 'car_color', 'car_color0', 'car_color_bh',
  'color_formula', 'color_formula_bh', 'color_number',
  'color_number_relation', 'color_brand', 'color_collection'
]

// ---- mysqldump value-list parser ----
// Handles single-quoted strings with backslash escapes (and '' doubling),
// NULL, integers (kept as BigInt when they exceed the safe range — the
// data carries snowflake ids) and floats.
const ESCAPES = { '0': '\0', b: '\b', n: '\n', r: '\r', t: '\t', Z: '\x1a', '\\': '\\', "'": "'", '"': '"' }

function parseNumberToken(tok) {
  if (/^-?\d+$/.test(tok)) {
    const digits = tok.replace('-', '')
    return digits.length > 15 ? BigInt(tok) : Number(tok)
  }
  const n = Number(tok)
  return Number.isNaN(n) ? tok : n
}

// Parse the tuple list starting at index i (just past "VALUES").
// Returns [rows, indexAfterSemicolon].
function parseValues(text, i) {
  const rows = []
  const len = text.length
  while (i < len) {
    const c = text[i]
    if (c === ';') return [rows, i + 1]
    if (c === ',') { i++; continue }
    if (c !== '(') { i++; continue }
    i++ // consume '('
    const row = []
    for (;;) {
      while (text[i] === ' ' || text[i] === '\n' || text[i] === '\r' || text[i] === '\t') i++
      if (text[i] === "'") {
        i++
        let s = ''
        for (;;) {
          const ch = text[i]
          if (ch === '\\') {
            const nxt = text[i + 1]
            s += Object.prototype.hasOwnProperty.call(ESCAPES, nxt) ? ESCAPES[nxt] : nxt
            i += 2
          } else if (ch === "'") {
            if (text[i + 1] === "'") { s += "'"; i += 2 } else { i++; break }
          } else {
            s += ch
            i++
          }
        }
        row.push(s)
      } else if (text.startsWith('NULL', i)) {
        row.push(null)
        i += 4
      } else {
        let j = i
        while (j < len && text[j] !== ',' && text[j] !== ')') j++
        row.push(parseNumberToken(text.slice(i, j).trim()))
        i = j
      }
      while (text[i] === ' ' || text[i] === '\n') i++
      if (text[i] === ',') { i++; continue }
      if (text[i] === ')') { i++; break }
      throw new Error(`unexpected character at offset ${i}: ${JSON.stringify(text[i])}`)
    }
    rows.push(row)
  }
  return [rows, i]
}

function main() {
  if (!existsSync(dataFile)) {
    console.error(`data file not found: ${dataFile}`)
    process.exit(1)
  }
  let raw = readFileSync(dataFile)
  if (dataFile.endsWith('.b64')) raw = Buffer.from(raw.toString('utf8').trim(), 'base64')
  if (dataFile.endsWith('.gz') || dataFile.endsWith('.b64')) raw = gunzipSync(raw)
  const text = raw.toString('utf8')

  // Collect rows per table, in file order.
  const byTable = new Map()
  const re = /INSERT INTO `([a-z_0-9]+)` VALUES /g
  let m
  while ((m = re.exec(text)) !== null) {
    const table = m[1]
    const [rows, end] = parseValues(text, re.lastIndex)
    re.lastIndex = end
    if (!TABLES.includes(table)) {
      console.log(`skip unknown table in dump: ${table} (${rows.length} rows)`)
      continue
    }
    if (!byTable.has(table)) byTable.set(table, [])
    byTable.get(table).push(...rows)
  }

  const db = new DatabaseSync(dbPath)
  db.exec(DDL)
  const results = []
  db.exec('BEGIN')
  try {
    for (const table of TABLES) {
      const rows = byTable.get(table) || []
      const { n } = db.prepare(`SELECT COUNT(*) AS n FROM ${table}`).get()
      if (Number(n) > 0) {
        results.push(`${table}: skipped (already has ${n} rows)`)
        continue
      }
      if (!rows.length) {
        results.push(`${table}: no rows in dump`)
        continue
      }
      const colCount = db.prepare(`SELECT COUNT(*) AS n FROM pragma_table_info('${table}')`).get().n
      const stmt = db.prepare(`INSERT INTO ${table} VALUES (${Array(Number(colCount)).fill('?').join(', ')})`)
      let inserted = 0
      for (const row of rows) {
        if (row.length !== Number(colCount)) {
          throw new Error(`${table}: row has ${row.length} values but the table has ${colCount} columns`)
        }
        stmt.run(...row)
        inserted++
      }
      results.push(`${table}: inserted ${inserted}`)
    }
    db.exec('COMMIT')
  } catch (e) {
    db.exec('ROLLBACK')
    throw e
  }
  console.log(`import into ${dbPath} complete:`)
  for (const line of results) console.log(`  ${line}`)
  db.close()
}

main()
