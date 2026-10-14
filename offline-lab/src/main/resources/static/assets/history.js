import {formatTime} from './report.js';
import {showError} from './api.js';

export class HistoryPanel {
    constructor(api, selectReport, selectTicket) {
        this.api = api; this.selectReport = selectReport; this.selectTicket = selectTicket; this.pages = {analysis:1,ticket:1}; this.generation = 0;
        ['analysis','ticket'].forEach(kind => {
            document.getElementById(kind+'Prev').addEventListener('click', () => { this.pages[kind]--; this.load(kind); });
            document.getElementById(kind+'Next').addEventListener('click', () => { this.pages[kind]++; this.load(kind); });
        });
        document.getElementById('refreshHistory').addEventListener('click', () => this.refresh());
    }
    async refresh() { this.generation++; await Promise.all([this.load('analysis'),this.load('ticket')]); }
    async load(kind) {
        const generation = this.generation, number = this.pages[kind];
        const prev = document.getElementById(kind+'Prev'), next = document.getElementById(kind+'Next');
        prev.disabled = next.disabled = true;
        try {
            const result = await this.api(`/api/${kind === 'analysis' ? 'analyses' : 'tickets'}/page?page=${number}&size=5`);
            if (generation !== this.generation || number !== this.pages[kind]) return;
            const list = document.getElementById(kind+'History'); list.replaceChildren();
            for (const item of result.items) {
                const entry = document.createElement('li'), button = document.createElement('button');
                button.type = 'button'; button.textContent = kind === 'analysis' ? `${item.assetIp} · ${item.riskLevel} · ${formatTime(item.time)}` : `${item.title} · ${item.status}`;
                button.addEventListener('click', () => kind === 'analysis' ? this.selectReport(item.id) : this.selectTicket(item));
                entry.append(button); list.append(entry);
            }
            if (!result.items.length) { const empty = document.createElement('li'); empty.textContent = '暂无记录'; list.append(empty); }
            document.getElementById(kind+'Page').textContent = `第 ${number} 页 · 共 ${result.total} 条`;
            prev.disabled = number <= 1; next.disabled = number*result.size >= result.total;
            document.getElementById('historyStatus').textContent = '';
        } catch(error) { if(generation===this.generation) showError(document.getElementById('historyStatus'),error); }
    }
}
