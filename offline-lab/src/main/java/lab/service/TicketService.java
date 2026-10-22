package lab.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.*;
import lab.config.PolicyProperties;
import lab.config.SecurityProperties;
import lab.contract.analysis.RiskLevel;
import lab.contract.api.ApiConstraints;
import lab.contract.api.PageResponse;
import lab.contract.ticket.*;
import lab.dao.TicketRepository;
import lab.entity.TicketPO;
import lab.enums.ErrorCode;
import lab.exception.LabException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 人工审核流程：同一报告唯一草稿，编辑和审批都必须提交用户已核对的版本。 */
@Service
@RequiredArgsConstructor
public class TicketService {
    private final AnalysisService analyses;
    private final TicketRepository repository;
    private final SecurityProperties security;
    private final PolicyProperties policy;
    private final PagePolicy pages;
    private final Clock clock;

    public TicketResponse draft(String analysisId) {
        var report = analyses.get(analysisId);
        // 重试直接返回原工单，包括已审批记录，不能以“重新生成”解锁内容。
        var existing = repository.findForAnalysis(analysisId);
        if (existing != null) return response(existing);
        var ticket = new TicketPO();
        ticket.setId("TICKET-" + UUID.randomUUID());
        ticket.setTenantId(security.tenantId());
        ticket.setAnalysisId(analysisId);
        ticket.setAssetIp(report.asset().ip());
        ticket.setTitle("资产 " + report.asset().ip() + " 风险复核与整改（模拟）");
        ticket.setStatus(TicketStatus.DRAFT);
        ticket.setRiskLevel(report.riskLevel());
        ticket.setCreatedAt(clock.instant());
        ticket.setNote("需要人工审核证据与修复方案；模拟平台不会调用真实工单系统。");
        var recommendations =
                report.knowledgeEvidence().stream().map(k -> k.title() + "：" + k.body()).toList();
        String remediation =
                recommendations.isEmpty()
                        ? "核实资产版本、暴露面和扫描结果，再制定整改方案。"
                        : String.join("\n\n", recommendations);
        ticket.setRemediation(
                remediation.length() <= ApiConstraints.MAX_REMEDIATION_LENGTH
                        ? remediation
                        : remediation.substring(0, ApiConstraints.MAX_REMEDIATION_LENGTH));
        ticket.setVersion(0L);
        try {
            repository.insertDraft(ticket);
        } catch (DataIntegrityViolationException ex) {
            // 先查后插仍可能竞争；数据库唯一约束决定胜者，失败者读取已提交的同一工单。
            var winner = repository.findForAnalysis(analysisId);
            if (winner == null) throw ex;
            return response(winner);
        }
        return response(ticket);
    }

    @Transactional
    public TicketResponse edit(String id, EditTicketRequest request) {
        var ticket = required(id);
        checkVersion(ticket, request.version());
        requireDraft(ticket);
        ticket.setTitle(request.title());
        ticket.setRemediation(request.remediation());
        ticket.setNote(request.note());
        if (!repository.update(ticket)) throw conflict();
        return get(id);
    }

    @Transactional
    public TicketResponse approve(ApproveRequest request) {
        if (!MessageDigest.isEqual(
                security.approvalCode().getBytes(StandardCharsets.UTF_8),
                request.approvalCode().getBytes(StandardCharsets.UTF_8)))
            throw new LabException(ErrorCode.FORBIDDEN, "审批码错误（仅用于本地教学，非生产认证）");
        var ticket = required(request.ticketId());
        checkVersion(ticket, request.version());
        requireDraft(ticket);
        ticket.setStatus(TicketStatus.APPROVED);
        ticket.setApprovedBy(security.reviewer());
        ticket.setApprovedAt(clock.instant());
        if (!repository.update(ticket)) throw conflict();
        return get(ticket.getId());
    }

    public TicketResponse get(String id) {
        return response(required(id));
    }

    public List<TicketResponse> list() {
        return repository.list(policy.legacyListLimit()).stream().map(this::response).toList();
    }

    public PageResponse<TicketResponse> page(long page, long size) {
        return page(page, size, null, null, null, null);
    }

    public PageResponse<TicketResponse> page(
            long page,
            long size,
            String q,
            TicketStatus status,
            RiskLevel riskLevel,
            String analysisId) {
        pages.validate(page, size);
        var result = repository.page(page, size, pages.keyword(q), status, riskLevel, analysisId);
        return new PageResponse<>(
                result.getRecords().stream().map(this::response).toList(),
                page,
                size,
                result.getTotalRow());
    }

    private TicketPO required(String id) {
        var ticket = repository.find(id);
        if (ticket == null) throw new IllegalArgumentException("工单不存在");
        return ticket;
    }

    private void requireDraft(TicketPO ticket) {
        if (ticket.getStatus() != TicketStatus.DRAFT)
            throw new LabException(ErrorCode.INVALID_STATE, "该工单已处理，不允许修改或重复审批");
    }

    /** 先比版本再校验状态，让并发旧请求得到 409，提示操作者重新核对最新内容。 */
    private void checkVersion(TicketPO ticket, Long version) {
        if (version == null) throw new IllegalArgumentException("必须提交草稿版本号 version");
        if (!version.equals(ticket.getVersion())) throw conflict();
    }

    private LabException conflict() {
        return new LabException(ErrorCode.VERSION_CONFLICT, "草稿已更新，请重新加载后审核");
    }

    private TicketResponse response(TicketPO t) {
        return new TicketResponse(
                t.getId(),
                t.getAnalysisId(),
                t.getAssetIp(),
                t.getTitle(),
                t.getStatus(),
                t.getRiskLevel(),
                t.getCreatedAt().toString(),
                t.getApprovedBy(),
                t.getApprovedAt() == null ? null : t.getApprovedAt().toString(),
                t.getNote(),
                t.getRemediation(),
                t.getVersion());
    }
}
