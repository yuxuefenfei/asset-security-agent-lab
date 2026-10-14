package lab.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("lab.security")
public record SecurityProperties(
        @NotBlank String apiToken,
        @NotBlank String approvalCode,
        @NotBlank String tenantId,
        @NotBlank String reviewer) {}
