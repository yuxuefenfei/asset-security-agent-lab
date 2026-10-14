package lab.contract.analysis;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(@NotBlank String ip, String question, AnalysisMode mode) {}
