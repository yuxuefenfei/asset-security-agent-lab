package lab.contract.dashboard;

/**
 * 工作台统计视图；资产、报告、工单按当前租户计数，知识为共享目录计数。
 *
 * @param assetCount 当前租户资产数
 * @param exposedAssetCount 其中标记公网暴露的资产数
 * @param reportCount 当前租户已保存报告数
 * @param highRiskReportCount 高风险报告数，同一资产多份报告分别计数
 * @param pendingTicketCount 草稿状态工单数，表示待人工审核
 * @param approvedTicketCount 已审批工单数，不代表已实际完成整改
 * @param knowledgeCount 共享知识目录条目数
 */
public record DashboardResponse(
        long assetCount,
        long exposedAssetCount,
        long reportCount,
        long highRiskReportCount,
        long pendingTicketCount,
        long approvedTicketCount,
        long knowledgeCount) {}
