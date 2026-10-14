package lab.contract.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ApproveRequest(
        @NotBlank String ticketId,
        @NotBlank String approvalCode,
        @NotNull @PositiveOrZero Long version) {}
