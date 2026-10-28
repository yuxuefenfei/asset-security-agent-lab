package lab.contract.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lab.contract.api.ApiConstraints;

/**
 * 完整替换草稿的可编辑内容；审批后不允许再编辑，旧版本请求会发生冲突。
 *
 * @param title 人工核对后的标题，不能为空
 * @param remediation 人工核对后的整改方案，不能为空
 * @param note 人工备注，为 null 时清空原备注
 * @param version 用户阅读并编辑的版本号，必须原样回传，不能自行递增
 */
public record EditTicketRequest(
        @NotBlank @Size(max = ApiConstraints.MAX_TICKET_TITLE_LENGTH) String title,
        @NotBlank @Size(max = ApiConstraints.MAX_REMEDIATION_LENGTH) String remediation,
        @Size(max = ApiConstraints.MAX_NOTE_LENGTH) String note,
        @NotNull @PositiveOrZero Long version) {}
