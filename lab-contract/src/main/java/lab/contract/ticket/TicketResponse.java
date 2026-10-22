package lab.contract.ticket;

import lab.contract.analysis.RiskLevel;

/** 工单当前内容与版本；编辑/审批必须回传已核对的 version，不能自行递增或沿用旧值。 */
public record TicketResponse(
        String id,
        String analysisId,
        String assetIp,
        String title,
        TicketStatus status,
        RiskLevel riskLevel,
        String createdAt,
        String approvedBy,
        String approvedAt,
        String note,
        String remediation,
        long version) {}
