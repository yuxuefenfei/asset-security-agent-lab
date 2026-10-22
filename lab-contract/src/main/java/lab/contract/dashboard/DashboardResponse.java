package lab.contract.dashboard;

public record DashboardResponse(
        long assetCount,
        long exposedAssetCount,
        long reportCount,
        long highRiskReportCount,
        long pendingTicketCount,
        long approvedTicketCount,
        long knowledgeCount) {}
