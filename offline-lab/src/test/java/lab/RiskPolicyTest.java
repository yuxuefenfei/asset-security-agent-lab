package lab;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import lab.config.RiskProperties;
import lab.contract.analysis.RiskLevel;
import lab.contract.asset.*;
import lab.contract.vulnerability.*;
import lab.service.RiskPolicy;
import org.junit.jupiter.api.Test;

class RiskPolicyTest {
    private final RiskPolicy policy = new RiskPolicy(new RiskProperties(10, 5, 100, 80, 50));

    @Test
    void thresholdBoundariesAreExplicit() {
        assertEquals(RiskLevel.HIGH, policy.level(80));
        assertEquals(RiskLevel.MEDIUM, policy.level(79));
        assertEquals(RiskLevel.MEDIUM, policy.level(50));
        assertEquals(RiskLevel.LOW, policy.level(49));
    }

    @Test
    void emptyVulnerabilitiesAndScoreCap() {
        var asset =
                new AssetResponse(
                        "a", "ip", "t", "h", "os", "b", Criticality.HIGH, true, "o", List.of());
        assertEquals(15, policy.score(asset, List.of()));
        var vulnerability =
                new VulnerabilityResponse(
                        "v",
                        "a",
                        "n",
                        Severity.CRITICAL,
                        9.9,
                        "c",
                        VulnerabilityStatus.OPEN,
                        "s",
                        "2026-10-13",
                        false);
        assertEquals(100, policy.score(asset, List.of(vulnerability)));
    }
}
