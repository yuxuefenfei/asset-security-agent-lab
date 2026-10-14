import {showError} from './api.js';
import {evidenceMarkup, formatTime} from './report.js';

export class TicketPanel {
    constructor(api, onChange) {
        this.api = api;
        this.onChange = onChange;
        this.generation = 0;
        this.elements = Object.fromEntries(['draft', 'refreshTicket', 'ticketHint', 'ticketStatus', 'ticketPreview', 'ticketMetadata', 'ticketEvidence', 'ticketEditor', 'ticketTitle', 'remediation', 'ticketNote', 'unsavedHint', 'saveTicket', 'code', 'approve'].map(id => [id, document.getElementById(id)]));
        this.elements.draft.addEventListener('click', () => this.generate());
        this.elements.refreshTicket.addEventListener('click', () => this.refresh());
        this.elements.saveTicket.addEventListener('click', () => this.save());
        this.elements.approve.addEventListener('click', () => this.approve());
        ['ticketTitle', 'remediation', 'ticketNote'].forEach(id => this.elements[id].addEventListener('input', () => this.sync()));
        this.reset(null);
    }

    reset(report) {
        this.generation++;
        this.controller?.abort();
        this.controller = new AbortController();
        this.report = report;
        this.ticket = null;
        this.busy = false;
        this.conflicted = false;
        this.elements.ticketPreview.hidden = true;
        this.elements.ticketStatus.textContent = '';
        this.elements.code.value = '';
        this.elements.ticketHint.textContent = report ? '点击生成草稿，审核完整内容后再审批。' : '分析完成后，可生成并预览整改工单草稿。';
        this.sync();
    }

    dirty() {
        return this.ticket && (this.elements.ticketTitle.value !== this.ticket.title || this.elements.remediation.value !== this.ticket.remediation || this.elements.ticketNote.value !== (this.ticket.note ?? ''));
    }

    sync() {
        const editable = this.ticket?.status === 'DRAFT';
        this.elements.draft.disabled = !this.report || this.busy;
        this.elements.refreshTicket.disabled = !this.ticket || this.busy;
        this.elements.ticketEditor.disabled = !editable || this.busy || this.conflicted;
        this.elements.saveTicket.disabled = !editable || !this.dirty() || this.busy || this.conflicted;
        this.elements.approve.disabled = !editable || !!this.dirty() || this.busy || this.conflicted;
        this.elements.code.disabled = !editable || this.busy || this.conflicted;
        this.elements.unsavedHint.textContent = !editable ? '已审批的内容已锁定。' : this.conflicted ? '请重新加载最新草稿，再进行审核。' : this.dirty() ? '存在未保存修改，请先保存，再审批。' : '审批将确认当前展示的草稿版本。';
        this.elements.ticketPreview.setAttribute('aria-busy', String(this.busy));
    }

    show(ticket) {
        if (!this.report || ticket.analysisId !== this.report.id) return;
        this.ticket = ticket;
        this.conflicted = false;
        this.elements.ticketPreview.hidden = false;
        this.elements.ticketTitle.value = ticket.title;
        this.elements.remediation.value = ticket.remediation;
        this.elements.ticketNote.value = ticket.note ?? '';
        this.elements.ticketMetadata.textContent = `${ticket.id} · ${ticket.status} · 版本 ${ticket.version} · 创建于 ${formatTime(ticket.createdAt)}` + (ticket.approvedAt ? ` · ${ticket.approvedBy} 于 ${formatTime(ticket.approvedAt)} 审批` : '');
        this.elements.ticketEvidence.innerHTML = evidenceMarkup(this.report);
        this.sync();
    }

    async perform(operation, success) {
        if (!this.report || this.busy) return;
        const generation = this.generation;
        this.busy = true;
        this.elements.ticketStatus.className = 'status';
        this.elements.ticketStatus.textContent = '正在处理…';
        this.sync();
        try {
            const ticket = await operation(this.controller.signal);
            if (generation !== this.generation) return;
            this.show(ticket);
            this.elements.ticketStatus.className = 'status success';
            this.elements.ticketStatus.textContent = success;
            this.onChange();
        } catch (error) {
            if (generation !== this.generation || error.name === 'AbortError') return;
            this.conflicted = error.status === 409;
            showError(this.elements.ticketStatus, error);
        } finally {
            if (generation === this.generation) {
                this.busy = false;
                this.sync();
            }
        }
    }

    generate() {
        const id = this.report?.id;
        return this.perform(signal => this.api('/api/tickets/draft', {
            method: 'POST',
            data: {analysisId: id},
            signal
        }), '草稿已加载，请审核内容。');
    }

    refresh() {
        const id = this.ticket?.id;
        if (!id) return;
        return this.perform(signal => this.api(`/api/tickets/${encodeURIComponent(id)}`, {signal}), '已加载最新草稿。');
    }

    save() {
        if (!this.ticket || this.elements.saveTicket.disabled) return;
        if (![this.elements.ticketTitle, this.elements.remediation, this.elements.ticketNote].every(input => input.reportValidity())) return;
        const id = this.ticket.id;
        const data = {
            title: this.elements.ticketTitle.value,
            remediation: this.elements.remediation.value,
            note: this.elements.ticketNote.value,
            version: this.ticket.version
        };
        return this.perform(signal => this.api(`/api/tickets/${encodeURIComponent(id)}`, {
            method: 'PUT',
            data,
            signal
        }), '草稿修改已保存，请再次审核后审批。');
    }

    approve() {
        if (!this.ticket || this.elements.approve.disabled) return;
        const approvalCode = this.elements.code.value;
        if (!approvalCode) {
            showError(this.elements.ticketStatus, new Error('请输入本地审批码'));
            return;
        }
        const data = {ticketId: this.ticket.id, approvalCode, version: this.ticket.version};
        return this.perform(signal => this.api('/api/tickets/approve', {
            method: 'POST',
            data,
            signal
        }), '人工审批已完成，工单内容已锁定（模拟）。');
    }
}
