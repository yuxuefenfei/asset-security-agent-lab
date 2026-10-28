package lab.contract.ticket;

/** 工单生命周期：DRAFT 可编辑和审批，APPROVED 锁定内容；已审批不等于整改已执行。 */
public enum TicketStatus {
    /** 待人工审核，可编辑并保存新版本。 */
    DRAFT,
    /** 已人工审批，工单内容锁定；不代表真实整改已经执行。 */
    APPROVED
}
