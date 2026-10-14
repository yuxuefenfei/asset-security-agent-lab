package lab.config;

import jakarta.validation.constraints.*;
import lab.contract.api.ApiConstraints;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("lab.risk")
public record RiskProperties(
        @PositiveOrZero int exposureBonus,
        @PositiveOrZero int highCriticalityBonus,
        @Positive @Max(ApiConstraints.MAX_RISK_SCORE) int maxRiskScore,
        @Positive int highThreshold,
        @Positive int mediumThreshold) {
    public static final int CVSS_SCALE = 10;

    @AssertTrue(message = "风险阈值必须满足 0 < medium < high <= max")
    public boolean isThresholdsValid() {
        return mediumThreshold < highThreshold && highThreshold <= maxRiskScore;
    }
}
