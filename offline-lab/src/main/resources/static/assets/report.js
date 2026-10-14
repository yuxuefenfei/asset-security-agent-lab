export const formatTime = value => new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    dateStyle: 'short',
    timeStyle: 'medium',
    hourCycle: 'h23'
}).format(new Date(value));

export const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
}[character]));

export function evidenceMarkup(report) {
    return `<p>资产 ${escapeHtml(report.asset.ip)} · 风险 ${escapeHtml(report.riskLevel)} / ${report.riskScore}</p><ul>${report.knowledgeEvidence.map(item => `<li>${escapeHtml(item.title)} <small>${escapeHtml(item.source)}</small></li>`).join('')}</ul>`;
}

export function renderReport(element, report) {
    element.innerHTML = `<div class="block"><strong>资产：</strong>${escapeHtml(report.asset.ip)} · ${escapeHtml(report.asset.hostname)}<br><strong>风险：</strong><span class="badge">${escapeHtml(report.riskLevel)} / ${report.riskScore}</span> · ${report.vulnerabilities.length} 条扫描记录</div><div class="block"><strong>分析说明：</strong><p>${escapeHtml(report.summary)}</p><p class="hint">${escapeHtml(report.warning)}</p></div><div class="block"><strong>知识库证据</strong>${evidenceMarkup(report)}</div><details><summary>工具调用追踪（${report.toolTrace.length}次）</summary><pre>${escapeHtml(JSON.stringify(report.toolTrace, null, 2))}</pre></details><details><summary>结构化报告</summary><pre>${escapeHtml(JSON.stringify(report, null, 2))}</pre></details>`;
}
