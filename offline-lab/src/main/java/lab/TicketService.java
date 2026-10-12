package lab;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟人工审批工单；持久化存储与正式身份认证属于后续练习。
 */
public final class TicketService {
    private final AgentService agent;
    private final Map<String, Map<String, Object>> tickets = new ConcurrentHashMap<>();
    private final Map<String, String> byAnalysis = new ConcurrentHashMap<>();
    private final String approvalCode = System.getenv().getOrDefault("LAB_APPROVAL_CODE", "local-approval-only");

    public TicketService(AgentService agent) {
        this.agent = agent;
    }

    public synchronized Map<String, Object> draft(String analysisId) {
        if (byAnalysis.containsKey(analysisId)) return tickets.get(byAnalysis.get(analysisId));
        Map<String, Object> report = agent.get(analysisId);
        String id = "TICKET-" + String.format("%04d", tickets.size() + 1);
        Map<String, Object> asset = Json.object(report.get("asset"));
        Map<String, Object> ticket = Json.map("id", id, "analysisId", analysisId, "assetIp", asset.get("ip"), "title", "资产 " + asset.get("ip") + " 风险复核与整改（模拟）", "status", "DRAFT", "riskLevel", report.get("riskLevel"), "createdAt", Instant.now().toString(), "approvedBy", null, "approvedAt", null, "note", "需要人工审核证据与修复方案；模拟平台不会调用真实工单系统。");
        tickets.put(id, ticket);
        byAnalysis.put(analysisId, id);
        return ticket;
    }

    public synchronized Map<String, Object> approve(String ticketId, String code) {
        if (!approvalCode.equals(code)) throw new SecurityException("审批码错误（仅用于本地教学，非生产认证）");
        Map<String, Object> t = tickets.get(ticketId);
        if (t == null) throw new IllegalArgumentException("工单不存在");
        if (!"DRAFT".equals(t.get("status"))) throw new IllegalStateException("该工单已处理，不允许重复审批");
        t.put("status", "APPROVED");
        t.put("approvedBy", "local-lab-reviewer");
        t.put("approvedAt", Instant.now().toString());
        return t;
    }

    public List<Map<String, Object>> list() {
        return new ArrayList<>(tickets.values());
    }
}
