package lab.contract.ticket;

import lab.contract.analysis.RiskLevel;

/**
 * 工单的当前内容与版本；修改和审批成功后，前端应使用响应中的新版本继续操作。
 *
 * @param id 工单唯一标识
 * @param analysisId 来源报告标识
 * @param assetIp 整改目标资产 IP
 * @param title 工单标题
 * @param status 草稿或已审批状态
 * @param riskLevel 创建时从报告复制的资产风险等级
 * @param createdAt 首次创建时刻的 ISO-8601 字符串
 * @param approvedBy 审批人展示名，未审批时为 null
 * @param approvedAt 审批完成时刻，未审批时为 null
 * @param note 人工审核备注，可为 null
 * @param remediation 整改方案，审批后锁定
 * @param version 服务端维护的乐观锁版本，编辑或审批请求必须回传已核对的值
 */
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
