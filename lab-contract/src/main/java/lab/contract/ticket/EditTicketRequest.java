package lab.contract.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lab.contract.api.ApiConstraints;

public record EditTicketRequest(
        @NotBlank @Size(max = ApiConstraints.MAX_TICKET_TITLE_LENGTH) String title,
        @NotBlank @Size(max = ApiConstraints.MAX_REMEDIATION_LENGTH) String remediation,
        @Size(max = ApiConstraints.MAX_NOTE_LENGTH) String note,
        @NotNull @PositiveOrZero Long version) {}
