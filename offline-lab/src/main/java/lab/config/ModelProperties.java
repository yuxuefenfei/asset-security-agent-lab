package lab.config;

import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("lab.model")
public record ModelProperties(
        @NotNull URI endpoint,
        @NotBlank String model,
        String apiKey,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout,
        @NotNull Duration totalTimeout,
        @Positive int maxRounds,
        @Positive int maxToolCalls,
        @Positive int maxResponseBytes,
        @Positive int maxConcurrentAnalyses,
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature) {
    @AssertTrue(message = "模型 URL 必须为 HTTP(S)，超时必须为正数")
    public boolean isSettingsValid() {
        return endpoint != null
                && Set.of("http", "https").contains(endpoint.getScheme())
                && connectTimeout != null
                && !connectTimeout.isNegative()
                && !connectTimeout.isZero()
                && requestTimeout != null
                && !requestTimeout.isNegative()
                && !requestTimeout.isZero()
                && totalTimeout != null
                && !totalTimeout.isNegative()
                && !totalTimeout.isZero();
    }
}
