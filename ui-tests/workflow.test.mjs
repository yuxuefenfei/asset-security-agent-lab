import { test, before, after } from 'node:test'
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const { chromium } = process.env.LAB_NODE_MODULES
  ? await import(pathToFileURL(path.join(process.env.LAB_NODE_MODULES, 'playwright/index.mjs')).href)
  : await import('playwright')
const token = 'learn-agent-local'
let server,
  browser,
  base,
  output = ''

before(
  async () => {
    const java = process.env.JAVA_HOME
      ? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
      : 'java'
    server = spawn(
      java,
      [
        '-jar',
        path.join(root, 'offline-lab/target/offline-lab.jar'),
        '--server.port=0',
        '--spring.datasource.url=jdbc:h2:mem:ui-tests',
        '--spring.datasource.username=sa',
        '--spring.datasource.password=',
        '--lab.security.api-token=' + token,
        '--lab.security.approval-code=local-approval-only',
      ],
      { cwd: root, windowsHide: true },
    )
    const ready = new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('Server startup timed out\n' + output)), 60000)
      server.once('error', (error) => {
        clearTimeout(timer)
        reject(error)
      })
      server.once('exit', (code) => {
        clearTimeout(timer)
        reject(new Error(`Server exited ${code}\n${output}`))
      })
      const inspect = (bytes) => {
        output = (output + bytes.toString()).slice(-30000)
        const match = output.match(/Tomcat started on port (\d+)/)
        if (match) {
          clearTimeout(timer)
          base = `http://127.0.0.1:${match[1]}`
          resolve()
        }
      }
      server.stdout.on('data', inspect)
      server.stderr.on('data', inspect)
    })
    await ready
    browser = await chromium.launch({
      headless: true,
      ...(process.env.LAB_BROWSER_CHANNEL ? { channel: process.env.LAB_BROWSER_CHANNEL } : {}),
    })
  },
  { timeout: 70000 },
)

after(async () => {
  await browser?.close()
  if (server && server.exitCode === null) {
    const exit = once(server, 'exit')
    server.kill()
    await exit
  }
})

async function pageForTest(route = '/analysis/new') {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
  await page.goto(base + '/#' + route)
  return page
}
async function analyze(page, ip = '192.0.2.10', navigate = true) {
  if (navigate) await page.goto(base + '/#/analysis/new?ip=' + ip)
  await page.locator('#analysisForm').waitFor()
  await page.selectOption('#ip', ip)
  await page.click('#run')
  await page.waitForURL(/#\/reports\/[^?]+$/)
  await page.locator('#analysisSummary').waitFor()
  assert.match(await page.locator('main').innerText(), new RegExp(ip.replaceAll('.', '\\.')))
  return page.url().split('/reports/')[1].split('?')[0]
}
async function generate(page) {
  await page.getByRole('button', { name: '生成工单草稿', exact: true }).click()
  await page.waitForURL(/#\/tickets\//)
  await page.locator('#ticketTitle').waitFor()
}
async function currentTicket(page) {
  const id = page.url().split('/tickets/')[1].split('?')[0]
  const response = await page.request.get(base + '/api/tickets/' + id, { headers: { 'X-Lab-Token': token } })
  assert.equal(response.status(), 200)
  return (await response.json()).data
}
test('cross-page preview, edit, save, approve and reopen locked ticket', async () => {
  const page = await pageForTest()
  try {
    const reportId = await analyze(page)
    await generate(page)
    assert.match(await page.inputValue('#ticketTitle'), /风险复核与整改/)
    assert.match(await page.inputValue('#remediation'), /DemoWeb/)
    const title = '人工审核后的工单标题'
    await page.fill('#ticketTitle', title)
    await page.fill('#remediation', '在维护窗口验证版本并升级，整改后复测。')
    assert.equal(await page.isDisabled('#approve'), true)
    await page.click('#saveTicket')
    await page.getByRole('status').filter({ hasText: '草稿修改已保存' }).waitFor()
    assert.equal((await currentTicket(page)).version, 1)
    await page.fill('#code', 'local-approval-only')
    await page.click('#approve')
    await page.getByRole('status').filter({ hasText: '人工审批已完成' }).waitFor()
    assert.equal(await page.isDisabled('#ticketTitle'), true)
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.screenshot({ path: path.join(root, 'offline-lab/target/ui-approved.png'), fullPage: true })
    await page.goto(base + '/#/reports/' + reportId)
    await page.getByRole('button', { name: '打开关联工单', exact: true }).click()
    await page.locator('#ticketTitle').waitFor()
    assert.equal(await page.isDisabled('#approve'), true)
    await page.reload()
    await page.locator('#ticketTitle').waitFor()
    assert.equal(await page.inputValue('#ticketTitle'), title)
    assert.equal(await page.isDisabled('#ticketTitle'), true)
    await page.goto(base + '/#/tickets?q=' + encodeURIComponent(title))
    await page.getByRole('link', { name: '查看详情', exact: true }).click()
    await page.locator('#ticketTitle').waitFor()
    assert.equal(await page.isDisabled('#approve'), true)
  } finally {
    await page.close()
  }
})
test('stale version blocks editing and approval until explicit refresh', async () => {
  const page = await pageForTest()
  try {
    await analyze(page)
    await generate(page)
    const ticket = await currentTicket(page)
    const response = await page.request.put(base + '/api/tickets/' + ticket.id, {
      headers: { 'X-Lab-Token': token },
      data: {
        title: '另一个审核者更新的标题',
        remediation: ticket.remediation,
        note: '更新',
        version: ticket.version,
      },
    })
    assert.equal(response.status(), 200)
    await page.fill('#code', 'local-approval-only')
    await page.click('#approve')
    await page.getByRole('alert').filter({ hasText: '当前版本已被其他操作修改' }).waitFor()
    assert.equal(await page.isDisabled('#approve'), true)
    assert.equal(await page.isDisabled('#ticketTitle'), true)
    await page.getByRole('button', { name: '重新加载最新草稿', exact: true }).click()
    await page.waitForFunction(
      () => document.getElementById('ticketTitle')?.value === '另一个审核者更新的标题',
    )
    assert.equal(await page.isDisabled('#ticketTitle'), false)
    await page.fill('#code', 'local-approval-only')
    assert.equal(await page.isDisabled('#approve'), false)
  } finally {
    await page.close()
  }
})
test('an old draft response cannot redirect or replace a newer report', async () => {
  const page = await pageForTest()
  try {
    await analyze(page)
    let release, received
    const pending = new Promise((resolve) => {
      release = resolve
    })
    const requested = new Promise((resolve) => {
      received = resolve
    })
    await page.route('**/api/tickets/draft', async (route) => {
      const response = await route.fetch()
      received()
      await pending
      try {
        await route.fulfill({ response })
      } catch {
        /* Old request is cancelled by navigation. */
      }
    })
    await page.getByRole('button', { name: '生成工单草稿', exact: true }).click()
    await requested
    await page.locator('nav[aria-label="主导航"] a[href="#/analysis/new"]').click()
    const current = await analyze(page, '192.0.2.20', false)
    release()
    await page.waitForTimeout(200)
    assert.ok(page.url().includes('/reports/' + current))
    assert.equal(await page.locator('#ticketTitle').count(), 0)
    assert.match(await page.locator('main').innerText(), /192\.0\.2\.20/)
    await page.unroute('**/api/tickets/draft')
    await generate(page)
    assert.equal((await currentTicket(page)).assetIp, '192.0.2.20')
  } finally {
    await page.close()
  }
})
test('model explanation is rendered as text and evidence and trace tabs retain route state', async () => {
  const page = await pageForTest()
  try {
    await page.route('**/api/analyses/*', async (route) => {
      const response = await route.fetch(),
        body = await response.json()
      body.data.summary = '<img src=x onerror="window.__unsafeRendered=true">'
      await route.fulfill({ response, json: body })
    })
    await analyze(page)
    assert.match(await page.locator('#analysisSummary').innerText(), /<img src=x/)
    assert.equal(await page.evaluate(() => window.__unsafeRendered), undefined)
    assert.equal(await page.locator('main img').count(), 0)
    await page.getByRole('link', { name: /工具轨迹/ }).click()
    await page.getByText('区分 Java 固定编排与模型主动请求').waitFor()
    assert.equal(await page.locator('.trace-list li').count(), 4)
    await page.reload()
    await page.locator('.trace-list').waitFor()
    assert.ok(page.url().includes('tab=trace'))
    await page.getByRole('link', { name: '知识证据', exact: true }).click()
    await page.locator('.evidence-item').first().waitFor()
    assert.ok((await page.locator('.evidence-item').count()) > 0)
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.screenshot({ path: path.join(root, 'offline-lab/target/ui-report.png'), fullPage: true })
  } finally {
    await page.close()
  }
})
test('dirty ticket guards navigation and preserves unsaved edits when staying', async () => {
  const page = await pageForTest()
  try {
    await analyze(page)
    await generate(page)
    await page.fill('#ticketTitle', '尚未保存的审核内容')
    page.once('dialog', (dialog) => dialog.dismiss())
    await page.locator('nav[aria-label="主导航"] a[href="#/assets"]').click()
    assert.equal(await page.inputValue('#ticketTitle'), '尚未保存的审核内容')
    assert.equal(await page.isDisabled('#approve'), true)
    page.once('dialog', (dialog) => dialog.accept())
    await page.locator('nav[aria-label="主导航"] a[href="#/assets"]').click()
    await page.waitForURL(/#\/assets$/)
  } finally {
    await page.close()
  }
})
test('server pagination, filters and browser back restore the same query', async () => {
  const page = await pageForTest('/reports')
  try {
    for (let i = 0; i < 11; i++) {
      const response = await page.request.post(base + '/api/analyze', {
        headers: { 'X-Lab-Token': token },
        data: { ip: '192.0.2.20', mode: 'demo', question: '分页验证' },
      })
      assert.equal(response.status(), 200)
    }
    await page.selectOption('#riskFilter', 'MEDIUM')
    await page.getByRole('button', { name: '查询', exact: true }).click()
    await page.locator('tbody tr').first().waitFor()
    assert.equal(await page.locator('tbody tr').count(), 10)
    const first = await page.locator('tbody tr').first().innerText()
    await page.getByRole('button', { name: '下一页', exact: true }).click()
    await page.waitForURL(/page=2/)
    await page.locator('tbody tr').first().waitFor()
    assert.notEqual(await page.locator('tbody tr').first().innerText(), first)
    await page.reload()
    await page.locator('tbody tr').first().waitFor()
    assert.equal(await page.inputValue('#riskFilter'), 'MEDIUM')
    await page.goBack()
    await page.locator('tbody tr').first().waitFor()
    assert.equal(await page.inputValue('#riskFilter'), 'MEDIUM')
    assert.equal(await page.locator('tbody tr').first().innerText(), first)
  } finally {
    await page.close()
  }
})
test('asset and knowledge browsing, empty state and retry use actual APIs', async () => {
  const page = await pageForTest('/assets')
  try {
    await page.locator('tbody tr').first().waitFor()
    assert.equal(await page.locator('tbody tr').count(), 10)
    await page.fill('#assetSearch', '不存在的训练资产')
    await page.getByRole('button', { name: '查询', exact: true }).click()
    await page.getByRole('heading', { name: '没有符合条件的资产' }).waitFor()
    await page.getByRole('button', { name: '清除筛选', exact: true }).click()
    await page.getByRole('link', { name: '192.0.2.10', exact: true }).click()
    await page.getByRole('heading', { name: '模拟漏洞记录', exact: true }).waitFor()
    assert.equal(await page.locator('table').first().locator('tbody tr').count(), 2)
    await page.goto(base + '/#/knowledge')
    await page.locator('.knowledge-card').first().waitFor()
    await page.locator('.knowledge-card').first().getByRole('link', { name: '阅读全文' }).click()
    await page.getByRole('heading', { name: '资料正文', exact: true }).waitFor()
    let fail = true
    await page.route('**/api/assets/page?**', async (route) => {
      if (fail) {
        fail = false
        await route.fulfill({
          status: 503,
          json: { success: false, error: '测试连接失败', code: 'UNAVAILABLE', traceId: 'ui-retry' },
        })
      } else await route.continue()
    })
    await page.goto(base + '/#/assets')
    await page.getByRole('alert').filter({ hasText: '测试连接失败' }).waitFor()
    await page.getByRole('button', { name: '重新加载', exact: true }).click()
    await page.locator('tbody tr').first().waitFor()
    assert.equal(await page.locator('tbody tr').count(), 10)
  } finally {
    await page.close()
  }
})
test('token change clears protected views, handles errors and restores connection', async () => {
  const page = await pageForTest('/settings')
  try {
    await page.getByText('核心服务连接正常').waitFor()
    await page.fill('#labToken', 'invalid-token')
    await page.getByRole('button', { name: '保存口令并检查连接' }).click()
    await page.getByRole('alert').filter({ hasText: '需要有效的 X-Lab-Token' }).waitFor()
    await page.locator('nav[aria-label="主导航"] a[href="#/"]').click()
    await page.getByRole('alert').filter({ hasText: '需要有效的 X-Lab-Token' }).waitFor()
    assert.equal(await page.locator('.metric').count(), 0)
    await page.getByRole('link', { name: '检查实验连接', exact: true }).click()
    await page.fill('#labToken', token)
    await page.getByRole('button', { name: '保存口令并检查连接' }).click()
    await page.getByText('核心服务连接正常').waitFor()
  } finally {
    await page.close()
  }
})
test('ticket status shortcuts preserve filters and open the current review version', async () => {
  const page = await pageForTest('/tickets')
  try {
    const reportResponse = await page.request.post(base + '/api/analyze', {
      headers: { 'X-Lab-Token': token },
      data: { ip: '192.0.2.30', mode: 'demo', question: '工单状态切换回归' },
    })
    assert.equal(reportResponse.status(), 200)
    const report = (await reportResponse.json()).data
    const draftResponse = await page.request.post(base + '/api/tickets/draft', {
      headers: { 'X-Lab-Token': token },
      data: { analysisId: report.id },
    })
    assert.equal(draftResponse.status(), 200)
    const draft = (await draftResponse.json()).data
    await page.fill('#ticketSearch', draft.id)
    await page.selectOption('#ticketRisk', 'HIGH')
    await page.getByRole('button', { name: '待审批', exact: true }).click()
    await page.waitForURL((url) => url.hash.includes('status=DRAFT'))
    await page.getByRole('link', { name: '审核草稿', exact: true }).waitFor()
    assert.equal(await page.locator('tbody tr').count(), 1)
    assert.equal(await page.locator('.pagination-controls').count(), 0)
    await page.getByRole('button', { name: '已审批', exact: true }).click()
    await page.getByRole('heading', { name: '当前没有符合条件的工单', exact: true }).waitFor()
    await page.goBack()
    await page.getByRole('link', { name: '审核草稿', exact: true }).waitFor()
    assert.equal(await page.inputValue('#ticketSearch'), draft.id)
    assert.equal(await page.inputValue('#ticketRisk'), 'HIGH')
    for (const width of [768, 414, 375, 320]) {
      await page.setViewportSize({ width, height: 900 })
      assert.ok(
        await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        'ticket page overflows at ' + width,
      )
    }
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.getByRole('link', { name: '审核草稿', exact: true }).click()
    await page.locator('#ticketTitle').waitFor()
    assert.equal(await page.inputValue('#ticketTitle'), draft.title)
    assert.equal(await page.isDisabled('#saveTicket'), true)
  } finally {
    await page.close()
  }
})
test('desktop and mobile workbench render without page overflow or script errors', async () => {
  const page = await pageForTest('/')
  const errors = []
  page.on('pageerror', (e) => errors.push(e.message))
  try {
    await page.locator('.metrics').waitFor()
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.screenshot({ path: path.join(root, 'offline-lab/target/ui-workbench.png'), fullPage: true })
    for (const width of [1920, 1440, 960, 768, 414, 375, 320]) {
      await page.setViewportSize({ width, height: 900 })
      assert.ok(
        await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        'page overflows at ' + width,
      )
    }
    await page.getByRole('button', { name: '切换导航', exact: true }).click()
    await page.locator('nav[aria-label="主导航"] a[href="#/assets"]').click()
    await page.locator('tbody tr').first().waitFor()
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.screenshot({ path: path.join(root, 'offline-lab/target/ui-mobile.png'), fullPage: true })
    const contrastFailures = await page.evaluate(() => {
      const canvas = document.createElement('canvas'),
        ctx = canvas.getContext('2d')
      canvas.width = 1
      canvas.height = 1
      const css = getComputedStyle(document.documentElement)
      function luminance(token) {
        ctx.fillStyle = css.getPropertyValue('--color-' + token).trim()
        ctx.fillRect(0, 0, 1, 1)
        const rgb = [...ctx.getImageData(0, 0, 1, 1).data]
          .slice(0, 3)
          .map((v) => v / 255)
          .map((v) => (v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4))
        return rgb[0] * 0.2126 + rgb[1] * 0.7152 + rgb[2] * 0.0722
      }
      return [
        ['ink', 'surface'],
        ['ink-2', 'paper-2'],
        ['muted', 'paper'],
        ['muted', 'paper-2'],
        ['sidebar-text', 'sidebar'],
        ['sidebar-muted', 'sidebar-active'],
        ['accent-ink', 'accent'],
        ['warning', 'warning-soft'],
        ['error', 'error-soft'],
        ['success', 'success-soft'],
        ['info', 'info-soft'],
        ['accent', 'accent-soft'],
      ].flatMap(([a, b]) => {
        const x = luminance(a),
          y = luminance(b),
          ratio = (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05)
        return ratio < 4.5 ? [a + '/' + b + ': ' + ratio.toFixed(2)] : []
      })
    })
    assert.deepEqual(contrastFailures, [], 'Text colors must meet WCAG 4.5:1')
    assert.deepEqual(errors, [])
  } finally {
    await page.close()
  }
})
