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

/** 人工审核流程的业务入口：一份报告对应唯一工单，只有草稿能编辑或审批。 编辑和审批都使用操作者已核对的版本；数据库乐观锁负责拦截校验后的并发写入。 */
@Service
@RequiredArgsConstructor
public class TicketService {
    private final AnalysisService analyses;
    private final TicketRepository repository;
    private final SecurityProperties security;
    private final PolicyProperties policy;
    private final PagePolicy pages;
    private final Clock clock;

    /** 从报告生成待人工审核的方案；重复或并发创建返回同一工单，已审批内容不会重新解锁。 */
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

    /** 完整替换草稿内容；先核对版本和可编辑状态，再用带旧版本条件的更新避免并发覆盖。 */
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

    /** 校验本地审批码及已核对版本，在同一事务中写入审批人、审批时刻并锁定工单内容。 */
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

    /** 读取当前租户的工单，并转换为前端可用的内容和版本。 */
    public TicketResponse get(String id) {
        return response(required(id));
    }

    /** 为旧调用方保留有限列表，页面应使用带筛选的分页方法。 */
    public List<TicketResponse> list() {
        return repository.list(policy.legacyListLimit()).stream().map(this::response).toList();
    }

    public PageResponse<TicketResponse> page(long page, long size) {
        return page(page, size, null, null, null, null);
    }

    /** 分页搜索当前租户工单；版本校验属于写入流程，列表只展示当前内容。 */
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

    /** 只在当前租户中查找工单；找不到时拒绝后续编辑或审批。 */
    private TicketPO required(String id) {
        var ticket = repository.find(id);
        if (ticket == null) throw new IllegalArgumentException("工单不存在");
        return ticket;
    }

    /** 业务状态守卫：已审批内容不可修改，也不能再次审批。 */
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

    /** 将持久化实体转为接口视图；未审批时间保持 null，时间统一输出为 ISO-8601 字符串。 */
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
