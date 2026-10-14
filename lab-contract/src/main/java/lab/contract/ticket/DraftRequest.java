package lab.contract.ticket;

import jakarta.validation.constraints.NotBlank;

public record DraftRequest(@NotBlank String analysisId) {}
