package lab.contract.ticket;

import lab.contract.analysis.RiskLevel;

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
