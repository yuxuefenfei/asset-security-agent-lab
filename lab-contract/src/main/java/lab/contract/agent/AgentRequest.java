package lab.contract.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lab.contract.api.ApiConstraints;

public record AgentRequest(
        @NotBlank String ip,
        @NotBlank @Size(max = ApiConstraints.MAX_QUESTION_LENGTH) String question) {}
