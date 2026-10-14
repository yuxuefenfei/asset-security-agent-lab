package lab.service;

import java.util.List;
import lab.config.RiskProperties;
import lab.contract.analysis.RiskLevel;
import lab.contract.asset.*;
import lab.contract.vulnerability.VulnerabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskPolicy {
    private final RiskProperties policy;

    public int score(AssetResponse asset, List<VulnerabilityResponse> vulnerabilities) {
        int highest =
                vulnerabilities.stream()
                        .mapToInt(v -> (int) Math.round(v.cvss() * RiskProperties.CVSS_SCALE))
                        .max()
                        .orElse(0);
        return Math.min(
                policy.maxRiskScore(),
                highest
                        + (asset.internetExposed() ? policy.exposureBonus() : 0)
                        + (asset.criticality() == Criticality.HIGH
                                ? policy.highCriticalityBonus()
                                : 0));
    }

    public RiskLevel level(int score) {
        return score >= policy.highThreshold()
                ? RiskLevel.HIGH
                : score >= policy.mediumThreshold() ? RiskLevel.MEDIUM : RiskLevel.LOW;
    }
}
