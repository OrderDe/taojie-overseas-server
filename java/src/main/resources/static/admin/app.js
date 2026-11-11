// Taojie overseas mall — admin UI. Plain JS, talks to /api/admin/** with the
// x-admin-token header. Works against both the Node and the Java backend
// (the Java one wraps responses in {code, message, data} — unwrapped here).
'use strict'

const $ = (id) => document.getElementById(id)
const state = {
  token: sessionStorage.getItem('tj_admin_token') || '',
  products: [],
  categories: [],
  editingProduct: null,
  editingCategory: null
}

function toast(msg) {
  const el = $('toast')
  el.textContent = msg
  el.hidden = false
  setTimeout(() => { el.hidden = true }, 2600)
}

async function api(path, opts = {}) {
  const res = await fetch(path, {
    ...opts,
    headers: { 'Content-Type': 'application/json', 'x-admin-token': state.token, ...(opts.headers || {}) }
  })
  let body = null
  try { body = await res.json() } catch { /* empty body */ }
  // Java backend wraps everything in CommonResult {code, message, data}.
  if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
    if (body.code !== 200) throw new Error(body.message || `请求失败 (${body.code})`)
    return body.data
  }
  if (!res.ok) throw new Error((body && body.error) || `请求失败 (${res.status})`)
  return body
}

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
const catName = (id) => {
  const c = state.categories.find((x) => x.id === id)
  return c ? (c.names.en || c.names.zh || c.id) : (id || '-')
}

// ---------------- login ----------------

async function tryLogin(token) {
  state.token = token
  await api('/api/admin/products')
  sessionStorage.setItem('tj_admin_token', token)
  $('loginView').hidden = true
  $('appView').hidden = false
  await loadAll()
}

$('loginBtn').addEventListener('click', async () => {
  const token = $('tokenInput').value.trim()
  $('loginErr').textContent = ''
  if (!token) { $('loginErr').textContent = '先输入管理员令牌'; return }
  try { await tryLogin(token) } catch (e) { $('loginErr').textContent = '令牌不对，或后端没启动 / 没设置 ADMIN_TOKEN' }
})
$('tokenInput').addEventListener('keydown', (e) => { if (e.key === 'Enter') $('loginBtn').click() })
$('logoutBtn').addEventListener('click', () => {
  sessionStorage.removeItem('tj_admin_token')
  location.reload()
})

// ---------------- tabs ----------------

document.querySelectorAll('.tab').forEach((btn) => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach((b) => b.classList.toggle('active', b === btn))
    for (const name of ['products', 'categories', 'rfq', 'config']) {
      $('tab-' + name).hidden = name !== btn.dataset.tab
    }
    if (btn.dataset.tab === 'rfq') loadRfq()
  })
})

// ---------------- data ----------------

async function loadAll() {
  state.categories = await api('/api/admin/categories')
  state.products = await api('/api/admin/products')
  renderCategoryOptions()
  renderProducts()
  renderCategories()
  const cfg = await api('/api/site-config')
  fillConfig(cfg)
}

async function reloadCatalogue() {
  state.categories = await api('/api/admin/categories')
  state.products = await api('/api/admin/products')
  renderCategoryOptions()
  renderProducts()
  renderCategories()
}

// ---------------- products ----------------

function renderCategoryOptions() {
  const opts = state.categories.map((c) => `<option value="${esc(c.id)}">${esc(c.names.en || c.names.zh || c.id)}</option>`).join('')
  $('f_category').innerHTML = opts || '<option value="">（暂无分类）</option>'
  $('pCatFilter').innerHTML = '<option value="">全部分类</option>' + opts
}

function renderProducts() {
  const kw = $('pSearch').value.trim().toLowerCase()
  const cat = $('pCatFilter').value
  const rows = state.products.filter((p) => {
    if (cat && p.categoryId !== cat) return false
    if (kw && !`${p.id} ${p.names.en || ''} ${p.names.zh || ''}`.toLowerCase().includes(kw)) return false
    return true
  })
  $('pRows').innerHTML = rows.map((p) => `
    <tr>
      <td>${p.image ? `<img class="thumb" src="${esc(p.image)}" alt="">` : ''}</td>
      <td>${p.id}</td>
      <td><b>${esc(p.names.en || p.names.zh || '')}</b>${p.names.zh ? `<br><span class="dim">${esc(p.names.zh)}</span>` : ''}</td>
      <td>${esc(catName(p.categoryId))}</td>
      <td>$${Number(p.price).toFixed(2)}</td>
      <td>${p.moq || '-'}</td>
      <td>${p.active ? '<span class="on">上架</span>' : '<span class="off">下架</span>'}</td>
      <td class="ops">
        <button class="btn small" data-edit="${p.id}">编辑</button>
        <button class="btn small danger" data-del="${p.id}">删除</button>
      </td>
    </tr>`).join('')
  $('pEmpty').hidden = state.products.length > 0
  $('pRows').querySelectorAll('[data-edit]').forEach((b) => b.addEventListener('click', () => openProductForm(Number(b.dataset.edit))))
  $('pRows').querySelectorAll('[data-del]').forEach((b) => b.addEventListener('click', () => deleteProduct(Number(b.dataset.del))))
}
$('pSearch').addEventListener('input', renderProducts)
$('pCatFilter').addEventListener('change', renderProducts)

function specsToText(specs) {
  return Object.entries(specs || {}).map(([k, v]) => `${k}: ${v}`).join('\n')
}
function textToSpecs(text) {
  const out = {}
  for (const line of text.split('\n')) {
    const m = line.match(/^\s*([^:=：]+)[:=：]\s*(.+)$/)
    if (m) out[m[1].trim()] = m[2].trim()
  }
  return out
}

function openProductForm(id) {
  const p = id ? state.products.find((x) => x.id === id) : null
  state.editingProduct = p ? p.id : null
  $('pFormTitle').textContent = p ? `编辑商品 #${p.id}` : '新建商品'
  $('f_en').value = p?.names.en || ''
  $('f_zh').value = p?.names.zh || ''
  $('f_es').value = p?.names.es || ''
  $('f_fr').value = p?.names.fr || ''
  $('f_pt').value = p?.names.pt || ''
  $('f_category').value = p?.categoryId || (state.categories[0]?.id || '')
  $('f_price').value = p?.price ?? ''
  $('f_otprice').value = p?.otPrice ?? ''
  $('f_moq').value = p?.moq ?? ''
  $('f_unit').value = p?.unit || 'pc'
  $('f_sales').value = p?.sales ?? ''
  $('f_sort').value = p?.sort ?? 0
  $('f_tag').value = p?.tag || ''
  $('f_source').value = p?.sourceId || ''
  $('f_image').value = p?.image || ''
  $('f_preview').hidden = !p?.image
  if (p?.image) $('f_preview').src = p.image
  $('f_blurb').value = p?.blurb || ''
  $('f_specs').value = specsToText(p?.specs)
  $('f_active').checked = p ? !!p.active : true
  $('pFormErr').textContent = ''
  $('pForm').hidden = false
  $('pForm').scrollIntoView({ behavior: 'smooth', block: 'start' })
}
$('pNew').addEventListener('click', () => openProductForm(null))
$('pCancel').addEventListener('click', () => { $('pForm').hidden = true })

$('f_upload').addEventListener('click', () => $('f_file').click())
$('f_file').addEventListener('change', async () => {
  const file = $('f_file').files[0]
  if (!file) return
  $('pFormErr').textContent = ''
  try {
    const dataBase64 = await new Promise((resolve, reject) => {
      const r = new FileReader()
      r.onload = () => resolve(String(r.result).split(',')[1])
      r.onerror = reject
      r.readAsDataURL(file)
    })
    const out = await api('/api/admin/upload', { method: 'POST', body: JSON.stringify({ filename: file.name, dataBase64 }) })
    $('f_image').value = out.url
    $('f_preview').src = out.url
    $('f_preview').hidden = false
    toast('图片已上传')
  } catch (e) { $('pFormErr').textContent = e.message }
})

$('pForm').addEventListener('submit', async (e) => {
  e.preventDefault()
  $('pFormErr').textContent = ''
  const payload = {
    names: { en: $('f_en').value.trim(), zh: $('f_zh').value.trim(), es: $('f_es').value.trim(), fr: $('f_fr').value.trim(), pt: $('f_pt').value.trim() },
    categoryId: $('f_category').value,
    price: Number($('f_price').value) || 0,
    otPrice: Number($('f_otprice').value) || 0,
    moq: Number($('f_moq').value) || 0,
    unit: $('f_unit').value.trim() || 'pc',
    sales: Number($('f_sales').value) || 0,
    sort: Number($('f_sort').value) || 0,
    tag: $('f_tag').value.trim(),
    sourceId: $('f_source').value.trim(),
    image: $('f_image').value.trim(),
    blurb: $('f_blurb').value.trim(),
    specs: textToSpecs($('f_specs').value),
    active: $('f_active').checked
  }
  try {
    if (state.editingProduct) {
      await api(`/api/admin/products/${state.editingProduct}`, { method: 'PUT', body: JSON.stringify(payload) })
      toast('商品已保存')
    } else {
      await api('/api/admin/products', { method: 'POST', body: JSON.stringify(payload) })
      toast('商品已新建')
    }
    $('pForm').hidden = true
    await reloadCatalogue()
  } catch (err) { $('pFormErr').textContent = err.message }
})

async function deleteProduct(id) {
  const p = state.products.find((x) => x.id === id)
  if (!confirm(`确定删除商品 #${id}「${p?.names.en || ''}」吗？删除后前台就看不到了。`)) return
  try {
    await api(`/api/admin/products/${id}`, { method: 'DELETE' })
    toast('已删除')
    await reloadCatalogue()
  } catch (e) { alert(e.message) }
}

// ---------------- categories ----------------

function renderCategories() {
  $('cRows').innerHTML = state.categories.map((c) => `
    <tr>
      <td>${esc(c.id)}</td>
      <td><b>${esc(c.names.en || '')}</b></td>
      <td>${esc(c.names.zh || '')}</td>
      <td>${c.count}</td>
      <td>${c.sort}</td>
      <td class="ops">
        <button class="btn small" data-cedit="${esc(c.id)}">编辑</button>
        <button class="btn small danger" data-cdel="${esc(c.id)}">删除</button>
      </td>
    </tr>`).join('')
  $('cRows').querySelectorAll('[data-cedit]').forEach((b) => b.addEventListener('click', () => {
    const c = state.categories.find((x) => x.id === b.dataset.cedit)
    if (!c) return
    state.editingCategory = c.id
    $('cFormTitle').textContent = `编辑分类 ${c.id}`
    $('c_id').value = c.id
    $('c_id').disabled = true
    $('c_sort').value = c.sort
    $('c_en').value = c.names.en || ''
    $('c_zh').value = c.names.zh || ''
    $('c_es').value = c.names.es || ''
    $('c_fr').value = c.names.fr || ''
    $('c_pt').value = c.names.pt || ''
    $('cFormErr').textContent = ''
  }))
  $('cRows').querySelectorAll('[data-cdel]').forEach((b) => b.addEventListener('click', async () => {
    if (!confirm(`确定删除分类「${b.dataset.cdel}」吗？`)) return
    try {
      await api(`/api/admin/categories/${encodeURIComponent(b.dataset.cdel)}`, { method: 'DELETE' })
      toast('分类已删除')
      await reloadCatalogue()
    } catch (e) { alert(e.message) }
  }))
}

$('cReset').addEventListener('click', () => {
  state.editingCategory = null
  $('cFormTitle').textContent = '新建分类'
  $('cForm').reset()
  $('c_id').disabled = false
  $('c_sort').value = 0
  $('cFormErr').textContent = ''
})

$('cForm').addEventListener('submit', async (e) => {
  e.preventDefault()
  $('cFormErr').textContent = ''
  const payload = {
    id: $('c_id').value.trim(),
    sort: Number($('c_sort').value) || 0,
    names: { en: $('c_en').value.trim(), zh: $('c_zh').value.trim(), es: $('c_es').value.trim(), fr: $('c_fr').value.trim(), pt: $('c_pt').value.trim() }
  }
  try {
    if (state.editingCategory) {
      await api(`/api/admin/categories/${encodeURIComponent(state.editingCategory)}`, { method: 'PUT', body: JSON.stringify(payload) })
      toast('分类已保存')
    } else {
      await api('/api/admin/categories', { method: 'POST', body: JSON.stringify(payload) })
      toast('分类已新建')
    }
    $('cReset').click()
    await reloadCatalogue()
  } catch (err) { $('cFormErr').textContent = err.message }
})

// ---------------- rfq ----------------

async function loadRfq() {
  try {
    const rows = await api('/api/admin/rfq')
    $('rRows').innerHTML = rows.map((r) => `
      <tr>
        <td>${esc(String(r.created_at || r.createTime || '').replace('T', ' ').slice(0, 16))}</td>
        <td><b>${esc(r.name)}</b></td>
        <td>${esc(r.contact)}</td>
        <td>${esc(r.product || '')}</td>
        <td>${esc(r.qty || '')}</td>
        <td class="msg">${esc(r.message || '')}</td>
        <td>${esc(r.lang || '')}</td>
      </tr>`).join('')
    $('rEmpty').hidden = rows.length > 0
  } catch (e) { alert(e.message) }
}
$('rRefresh').addEventListener('click', loadRfq)

// ---------------- site config ----------------

function fillConfig(cfg) {
  $('cfg_theme').value = String(cfg.defaultTheme || 'h').toLowerCase()
  $('cfg_lang').value = cfg.defaultLanguage || 'en'
  $('cfg_hero').value = cfg.heroTitle || ''
  $('cfg_sub').value = cfg.heroSubtitle || ''
  $('cfg_ann').value = cfg.announcement || ''
  $('cfg_wa').value = cfg.whatsapp || ''
  $('cfg_email').value = cfg.email || ''
  $('cfg_cur').value = cfg.currency || '$'
  $('cfg_rfq').checked = cfg.showRfq !== false
}

$('cfgForm').addEventListener('submit', async (e) => {
  e.preventDefault()
  $('cfgErr').textContent = ''
  const payload = {
    defaultTheme: $('cfg_theme').value,
    defaultLanguage: $('cfg_lang').value,
    heroTitle: $('cfg_hero').value,
    heroSubtitle: $('cfg_sub').value,
    announcement: $('cfg_ann').value,
    whatsapp: $('cfg_wa').value,
    email: $('cfg_email').value,
    currency: $('cfg_cur').value,
    showRfq: $('cfg_rfq').checked
  }
  try {
    await api('/api/admin/site-config', { method: 'PUT', body: JSON.stringify(payload) })
    toast('配置已保存')
  } catch (err) { $('cfgErr').textContent = err.message }
})

// ---------------- boot ----------------

if (state.token) {
  tryLogin(state.token).catch(() => {
    sessionStorage.removeItem('tj_admin_token')
    state.token = ''
  })
}
