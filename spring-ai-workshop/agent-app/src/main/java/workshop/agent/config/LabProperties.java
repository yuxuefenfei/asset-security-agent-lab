package workshop.agent.config;

import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Duration;
import lab.contract.agent.ToolBackend;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("lab")
public record LabProperties(
        @NotNull URI coreUrl,
        @NotBlank String apiToken,
        @NotBlank String allowedIp,
        @Positive int maxQueryLength,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout,
        @NotNull ToolBackend toolBackend) {}
