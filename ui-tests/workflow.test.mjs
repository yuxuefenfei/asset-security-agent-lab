import {test, before, after} from 'node:test';
import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {once} from 'node:events';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const {chromium} = process.env.LAB_NODE_MODULES
    ? await import(pathToFileURL(path.join(process.env.LAB_NODE_MODULES, 'playwright/index.mjs')).href)
    : await import('playwright');
const token = 'learn-agent-local';
let server, browser, base, output = '';

before(async () => {
    const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
    server = spawn(java, ['-jar', path.join(root, 'offline-lab/target/offline-lab.jar'), '--server.port=0',
        '--spring.datasource.url=jdbc:h2:mem:ui-tests', '--spring.datasource.username=sa', '--spring.datasource.password=',
        '--lab.security.api-token=' + token, '--lab.security.approval-code=local-approval-only'], {cwd:root, windowsHide:true});
    const ready = new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('Server startup timed out\n' + output)), 60000);
        server.once('error', error => {clearTimeout(timer); reject(error);});
        server.once('exit', code => {clearTimeout(timer); reject(new Error(`Server exited ${code}\n${output}`));});
        const inspect = bytes => {
            output = (output + bytes.toString()).slice(-30000);
            const match = output.match(/Tomcat started on port (\d+)/);
            if (match) {clearTimeout(timer); base = `http://127.0.0.1:${match[1]}`; resolve();}
        };
        server.stdout.on('data', inspect); server.stderr.on('data', inspect);
    });
    await ready;
    browser = await chromium.launch({headless:true, ...(process.env.LAB_BROWSER_CHANNEL ? {channel:process.env.LAB_BROWSER_CHANNEL} : {})});
}, {timeout:70000});

after(async () => {
    await browser?.close();
    if (server && server.exitCode === null) {const exit = once(server,'exit'); server.kill(); await exit;}
});

async function pageForTest() {
    const page = await browser.newPage();
    await page.goto(base);
    await page.locator('#analysisForm').waitFor();
    return page;
}
async function analyze(page, ip = '192.0.2.10') {
    await page.selectOption('#ip', ip); await page.click('#run');
    await page.waitForFunction(() => !document.getElementById('run').disabled && document.getElementById('analysisStatus').textContent.includes('分析完成'));
    assert.match(await page.locator('#result').innerText(), new RegExp(ip.replaceAll('.','\\.')));
}
async function generate(page) {
    await page.click('#draft'); await page.locator('#ticketPreview').waitFor({state:'visible'});
    await page.waitForFunction(() => !document.getElementById('draft').disabled);
}
async function currentTicket(page) {
    const id = (await page.locator('#ticketMetadata').innerText()).split(' · ')[0];
    const response = await page.request.get(`${base}/api/tickets/${id}`, {headers:{'X-Lab-Token':token}});
    assert.equal(response.status(),200); return (await response.json()).data;
}

test('preview, edit, save, approve, and reopen locked ticket', async () => {
    const page = await pageForTest();
    try {
        await analyze(page); await generate(page);
        assert.match(await page.inputValue('#ticketTitle'), /风险复核与整改/);
        assert.match(await page.inputValue('#remediation'), /DemoWeb/);
        assert.match(await page.locator('#ticketEvidence').innerText(), /HIGH/);
        const title = '人工审核后的工单标题';
        await page.fill('#ticketTitle', title); await page.fill('#remediation','在维护窗口验证版本并升级，整改后复测。');
        assert.equal(await page.isDisabled('#approve'),true);
        await page.click('#saveTicket');
        await page.waitForFunction(() => document.getElementById('ticketStatus').textContent.includes('修改已保存'));
        assert.equal((await currentTicket(page)).version,1);
        await page.fill('#code','local-approval-only'); await page.click('#approve');
        await page.waitForFunction(() => document.getElementById('ticketStatus').textContent.includes('审批已完成'));
        assert.equal(await page.isDisabled('#ticketTitle'),true);
        await page.screenshot({path:path.join(root,'offline-lab/target/ui-approved.png'),fullPage:true});
        await page.click('#draft');
        await page.waitForFunction(() => !document.getElementById('draft').disabled);
        assert.equal(await page.isDisabled('#approve'),true);
        await page.reload();
        await page.getByRole('button',{name: title + ' · APPROVED',exact:true}).click();
        await page.locator('#ticketPreview').waitFor({state:'visible'});
        assert.equal(await page.inputValue('#ticketTitle'),title);
        assert.equal(await page.isDisabled('#ticketTitle'),true);
        assert.equal(await page.isDisabled('#approve'),true);
    } finally {await page.close();}
});

test('stale version blocks approval until explicit refresh', async () => {
    const page = await pageForTest();
    try {
        await analyze(page); await generate(page);
        const ticket = await currentTicket(page);
        const update = await page.request.put(`${base}/api/tickets/${ticket.id}`,{headers:{'X-Lab-Token':token},data:{title:'另一个审核者更新的标题',remediation:ticket.remediation,note:'更新',version:ticket.version}});
        assert.equal(update.status(),200);
        await page.fill('#code','local-approval-only'); await page.click('#approve');
        await page.waitForFunction(() => document.getElementById('ticketStatus').textContent.includes('草稿已更新'));
        assert.equal(await page.isDisabled('#approve'),true);
        await page.click('#refreshTicket');
        await page.waitForFunction(() => document.getElementById('ticketStatus').textContent.includes('最新草稿'));
        assert.equal(await page.inputValue('#ticketTitle'),'另一个审核者更新的标题');
        assert.equal(await page.isDisabled('#approve'),false);
    } finally {await page.close();}
});

test('an old draft response cannot replace a new analysis', async () => {
    const page = await pageForTest();
    try {
        await analyze(page);
        let release, received;
        const pending = new Promise(resolve => {release = resolve;});
        const requested = new Promise(resolve => {received = resolve;});
        await page.route('**/api/tickets/draft',async route => {
            const response = await route.fetch(); received(); await pending;
            try {await route.fulfill({response});} catch { /* 请求被新分析取消。 */ }
        });
        await page.click('#draft'); await requested;
        await analyze(page,'192.0.2.20'); release();
        await page.waitForTimeout(150);
        assert.equal(await page.locator('#ticketPreview').isVisible(),false);
        assert.equal(await page.isDisabled('#approve'),true);
        assert.match(await page.locator('#result').innerText(),/192\.0\.2\.20/);
        await page.unroute('**/api/tickets/draft'); await generate(page);
        assert.equal((await currentTicket(page)).assetIp,'192.0.2.20');
    } finally {await page.close();}
});

test('model explanation is rendered as text', async () => {
    const page = await pageForTest();
    try {
        await page.route('**/api/analyze',async route => {
            const response = await route.fetch(); const body = await response.json();
            body.data.summary = '<img src=x onerror="window.__unsafeRendered=true">';
            await route.fulfill({response,json:body});
        });
        await analyze(page);
        assert.match(await page.locator('#result').innerText(), /<img src=x/);
        assert.equal(await page.evaluate(() => window.__unsafeRendered),undefined);
        assert.equal(await page.locator('#result img').count(),0);
    } finally {await page.close();}
});
