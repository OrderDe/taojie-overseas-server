// Taojie overseas mall — admin UI. Plain JS, talks to /api/admin/** with the
// x-admin-token header. Works against both the Node and the Java backend
// (the Java one wraps responses in {code, message, data} — unwrapped here).
'use strict'

const $ = (id) => document.getElementById(id)
const state = {
  token: sessionStorage.getItem('tj_admin_token') || '',
  user: null,
  products: [],
  categories: [],
  users: [],
  roles: [],
  permDefs: [],
  editingProduct: null,
  editingCategory: null,
  editingUser: null,
  editingRole: null
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

const can = (perm) => !!state.user && (state.user.permissions.includes('*') || state.user.permissions.includes(perm))

async function enterApp() {
  $('loginView').hidden = true
  $('appView').hidden = false
  $('whoami').textContent = `${state.user.displayName || state.user.username} · ${state.user.roleName || ''}`
  applyPermissions()
  await loadAll()
}

// Tabs follow the role's menu permissions: no permission, no tab.
function applyPermissions() {
  const tabPerm = { products: 'products', categories: 'categories', rfq: 'rfq', config: 'config', users: 'users' }
  let firstVisible = null
  document.querySelectorAll('.tab').forEach((btn) => {
    const visible = can(tabPerm[btn.dataset.tab])
    btn.hidden = !visible
    if (visible && !firstVisible) firstVisible = btn
  })
  const active = document.querySelector('.tab.active')
  if (firstVisible && (!active || active.hidden)) firstVisible.click()
}

$('loginBtn').addEventListener('click', async () => {
  const username = $('loginUser').value.trim()
  const password = $('loginPass').value
  $('loginErr').textContent = ''
  if (!username || !password) { $('loginErr').textContent = '输入账号和密码'; return }
  try {
    const out = await api('/api/admin/login', { method: 'POST', body: JSON.stringify({ username, password }) })
    state.token = out.token
    state.user = out.user
    sessionStorage.setItem('tj_admin_token', state.token)
    await enterApp()
  } catch (e) { $('loginErr').textContent = '账号或密码不对，或后端没启动' }
})
$('loginPass').addEventListener('keydown', (e) => { if (e.key === 'Enter') $('loginBtn').click() })
$('logoutBtn').addEventListener('click', async () => {
  try { await api('/api/admin/logout', { method: 'POST' }) } catch { /* session already gone */ }
  sessionStorage.removeItem('tj_admin_token')
  location.reload()
})

// ---------------- tabs ----------------

document.querySelectorAll('.tab').forEach((btn) => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach((b) => b.classList.toggle('active', b === btn))
    for (const name of ['products', 'categories', 'rfq', 'config', 'users']) {
      $('tab-' + name).hidden = name !== btn.dataset.tab
    }
    if (btn.dataset.tab === 'rfq') loadRfq()
    if (btn.dataset.tab === 'users') loadUsers()
  })
})

// ---------------- data ----------------

async function loadAll() {
  // Only fetch what the role may see — the API would 403 the rest anyway.
  if (can('categories') || can('products')) {
    try { state.categories = await api('/api/admin/categories') } catch { /* keep empty */ }
  }
  if (can('products')) {
    try { state.products = await api('/api/admin/products') } catch { /* keep empty */ }
    renderCategoryOptions()
    renderProducts()
  }
  if (can('categories')) renderCategories()
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

// ---------------- users & roles ----------------

async function loadUsers() {
  try {
    state.permDefs = await api('/api/admin/permissions')
    state.roles = await api('/api/admin/roles')
    state.users = await api('/api/admin/users')
    renderRoleOptions()
    renderUsers()
    renderRoles()
    if (!state.editingRole) renderPermChecks([])
  } catch (e) { alert(e.message) }
}

function renderRoleOptions() {
  $('u_role').innerHTML = state.roles.map((r) => `<option value="${r.id}">${esc(r.name)}</option>`).join('')
}

function renderUsers() {
  $('uRows').innerHTML = state.users.map((u) => `
    <tr>
      <td><b>${esc(u.username)}</b></td>
      <td>${esc(u.displayName || '')}</td>
      <td>${esc(u.roleName || '')}</td>
      <td>${u.active ? '<span class="on">启用</span>' : '<span class="off">禁用</span>'}</td>
      <td class="ops">
        <button class="btn small" data-uedit="${u.id}">编辑</button>
        <button class="btn small danger" data-udel="${u.id}">删除</button>
      </td>
    </tr>`).join('')
  $('uRows').querySelectorAll('[data-uedit]').forEach((b) => b.addEventListener('click', () => {
    const u = state.users.find((x) => x.id === Number(b.dataset.uedit))
    if (!u) return
    state.editingUser = u.id
    $('uFormTitle').textContent = `编辑用户 ${u.username}`
    $('u_username').value = u.username
    $('u_username').disabled = true
    $('u_display').value = u.displayName || ''
    $('u_password').value = ''
    $('u_role').value = u.roleId
    $('u_active').checked = !!u.active
    $('uFormErr').textContent = ''
  }))
  $('uRows').querySelectorAll('[data-udel]').forEach((b) => b.addEventListener('click', async () => {
    const u = state.users.find((x) => x.id === Number(b.dataset.udel))
    if (!confirm(`确定删除用户「${u?.username}」吗？`)) return
    try {
      await api(`/api/admin/users/${b.dataset.udel}`, { method: 'DELETE' })
      toast('用户已删除')
      await loadUsers()
    } catch (e) { alert(e.message) }
  }))
}

$('uReset').addEventListener('click', () => {
  state.editingUser = null
  $('uFormTitle').textContent = '新建用户'
  $('uForm').reset()
  $('u_username').disabled = false
  $('u_active').checked = true
  $('uFormErr').textContent = ''
})

$('uForm').addEventListener('submit', async (e) => {
  e.preventDefault()
  $('uFormErr').textContent = ''
  const payload = {
    displayName: $('u_display').value.trim(),
    roleId: Number($('u_role').value),
    active: $('u_active').checked
  }
  const pw = $('u_password').value
  if (pw) payload.password = pw
  try {
    if (state.editingUser) {
      await api(`/api/admin/users/${state.editingUser}`, { method: 'PUT', body: JSON.stringify(payload) })
      toast('用户已保存')
    } else {
      payload.username = $('u_username').value.trim()
      if (!payload.password) { $('uFormErr').textContent = '新建用户必须设置密码'; return }
      await api('/api/admin/users', { method: 'POST', body: JSON.stringify(payload) })
      toast('用户已新建')
    }
    $('uReset').click()
    await loadUsers()
  } catch (err) { $('uFormErr').textContent = err.message }
})

function permLabel(key) {
  if (key === '*') return '全部权限'
  const def = state.permDefs.find((p) => p.key === key)
  return def ? def.label : key
}

function renderPermChecks(selected) {
  const sel = selected || []
  const star = `<label class="check"><input type="checkbox" value="*" ${sel.includes('*') ? 'checked' : ''}> 全部权限（超级管理员）</label>`
  $('r_perms').innerHTML = star + state.permDefs.map((p) =>
    `<label class="check"><input type="checkbox" value="${p.key}" ${sel.includes(p.key) ? 'checked' : ''}> ${esc(p.label)}</label>`).join('')
}
function collectPerms() {
  return [...$('r_perms').querySelectorAll('input:checked')].map((i) => i.value)
}

function renderRoles() {
  $('rRows').innerHTML = state.roles.map((r) => `
    <tr>
      <td><b>${esc(r.name)}</b>${r.builtin ? ' <span class="dim">内置</span>' : ''}</td>
      <td>${r.permissions.map(permLabel).join('、') || '-'}</td>
      <td>${r.userCount}</td>
      <td class="ops">
        <button class="btn small" data-redit="${r.id}">编辑</button>
        <button class="btn small danger" data-rdel="${r.id}">删除</button>
      </td>
    </tr>`).join('')
  $('rRows').querySelectorAll('[data-redit]').forEach((b) => b.addEventListener('click', () => {
    const r = state.roles.find((x) => x.id === Number(b.dataset.redit))
    if (!r) return
    state.editingRole = r.id
    $('rFormTitle').textContent = `编辑角色 ${r.name}`
    $('r_name').value = r.name
    renderPermChecks(r.permissions)
    if (r.builtin) {
      $('r_perms').querySelectorAll('input').forEach((i) => { i.disabled = true })
      $('rFormErr').textContent = '内置超级管理员角色的权限固定为全部，不能改'
    } else {
      $('rFormErr').textContent = ''
    }
  }))
  $('rRows').querySelectorAll('[data-rdel]').forEach((b) => b.addEventListener('click', async () => {
    const r = state.roles.find((x) => x.id === Number(b.dataset.rdel))
    if (!confirm(`确定删除角色「${r?.name}」吗？`)) return
    try {
      await api(`/api/admin/roles/${b.dataset.rdel}`, { method: 'DELETE' })
      toast('角色已删除')
      $('rReset').click()
      await loadUsers()
    } catch (e) { alert(e.message) }
  }))
}

$('rReset').addEventListener('click', () => {
  state.editingRole = null
  $('rFormTitle').textContent = '新建角色'
  $('rForm').reset()
  renderPermChecks([])
  $('rFormErr').textContent = ''
})

$('rForm').addEventListener('submit', async (e) => {
  e.preventDefault()
  $('rFormErr').textContent = ''
  const payload = { name: $('r_name').value.trim(), permissions: collectPerms() }
  try {
    if (state.editingRole) {
      await api(`/api/admin/roles/${state.editingRole}`, { method: 'PUT', body: JSON.stringify(payload) })
      toast('角色已保存')
    } else {
      await api('/api/admin/roles', { method: 'POST', body: JSON.stringify(payload) })
      toast('角色已新建')
    }
    $('rReset').click()
    await loadUsers()
  } catch (err) { $('rFormErr').textContent = err.message }
})

// ---------------- boot ----------------

if (state.token) {
  api('/api/admin/me').then((me) => {
    state.user = me
    return enterApp()
  }).catch(() => {
    sessionStorage.removeItem('tj_admin_token')
    state.token = ''
  })
}
