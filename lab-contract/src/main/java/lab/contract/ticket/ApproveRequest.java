package lab.contract.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 确认当前版本工单的人工审批请求；审批码仅用于本地教学，不代表生产身份认证。
 *
 * @param ticketId 待审批工单标识
 * @param approvalCode 本地审批口令，只提交给服务端校验，不写入工单
 * @param version 已经人工核对的草稿版本，防止批准他人刚修改的内容
 */
public record ApproveRequest(
        @NotBlank String ticketId,
        @NotBlank String approvalCode,
        @NotNull @PositiveOrZero Long version) {}
