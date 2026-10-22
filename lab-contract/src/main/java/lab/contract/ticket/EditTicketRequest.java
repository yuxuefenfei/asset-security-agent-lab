package lab.contract.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lab.contract.api.ApiConstraints;

/** 完整替换草稿可编辑内容；note 为 null 时清空备注，version 用于乐观锁。 */
public record EditTicketRequest(
        @NotBlank @Size(max = ApiConstraints.MAX_TICKET_TITLE_LENGTH) String title,
        @NotBlank @Size(max = ApiConstraints.MAX_REMEDIATION_LENGTH) String remediation,
        @Size(max = ApiConstraints.MAX_NOTE_LENGTH) String note,
        @NotNull @PositiveOrZero Long version) {}
