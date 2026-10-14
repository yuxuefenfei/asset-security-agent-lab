import {createApi, showError} from './api.js';
import {renderReport} from './report.js';
import {TicketPanel} from './ticket.js';
import {HistoryPanel} from './history.js';

const api = createApi(() => document.getElementById('labToken').value);
let generation = 0, controller;
const status = document.getElementById('analysisStatus'), result = document.getElementById('result'),
    run = document.getElementById('run');
const ticket = new TicketPanel(api, () => history.refresh());
const history = new HistoryPanel(api, id => loadReport(id), item => loadReport(item.analysisId, item));

function begin() {
    generation++;
    controller?.abort();
    controller = new AbortController();
    ticket.reset(null);
    run.disabled = true;
    status.className = 'status';
    status.textContent = '正在加载分析结果…';
    return generation;
}

function show(report, current) {
    if (current !== generation) return false;
    renderReport(result, report);
    ticket.reset(report);
    status.textContent = '分析完成，请审核风险与证据。';
    return true;
}

async function loadReport(id, selectedTicket) {
    const current = begin();
    try {
        const report = await api(`/api/analyses/${encodeURIComponent(id)}`, {signal: controller.signal});
        if (show(report, current) && selectedTicket) {
            const latest = await api(`/api/tickets/${encodeURIComponent(selectedTicket.id)}`, {signal: controller.signal});
            if (current === generation) ticket.show(latest);
        }
    } catch (error) {
        if (current === generation && error.name !== 'AbortError') showError(status, error);
    } finally {
        if (current === generation) run.disabled = false;
    }
}

document.getElementById('analysisForm').addEventListener('submit', async event => {
    event.preventDefault();
    const current = begin();
    result.textContent = '正在执行分析…';
    try {
        const report = await api('/api/analyze', {
            method: 'POST',
            signal: controller.signal,
            data: {
                ip: document.getElementById('ip').value,
                mode: document.getElementById('mode').value,
                question: document.getElementById('question').value
            }
        });
        if (show(report, current)) history.refresh();
    } catch (error) {
        if (current === generation && error.name !== 'AbortError') {
            result.textContent = '分析未完成';
            showError(status, error);
        }
    } finally {
        if (current === generation) run.disabled = false;
    }
});
document.getElementById('labToken').addEventListener('change', () => {
    begin();
    run.disabled = false;
    status.textContent = '口令已更新，请重新选择或创建分析。';
    history.refresh();
});
history.refresh();
