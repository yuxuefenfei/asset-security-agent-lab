package lab.service;

import java.util.List;
import lab.config.RiskProperties;
import lab.contract.analysis.RiskLevel;
import lab.contract.asset.*;
import lab.contract.vulnerability.VulnerabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 可单独测试的教学风险规则；不调用模型或数据库，只根据输入快照计算分数与等级。 */
@Component
@RequiredArgsConstructor
public class RiskPolicy {
    private final RiskProperties policy;

    /** 教学启发式：最高 CVSS 加资产属性；包含已解决记录，无漏洞也可能有资产加分。 */
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

    /** 先判断高风险阈值，再判断中风险阈值；阈值顺序在配置启动校验中保证。 */
    public RiskLevel level(int score) {
        return score >= policy.highThreshold()
                ? RiskLevel.HIGH
                : score >= policy.mediumThreshold() ? RiskLevel.MEDIUM : RiskLevel.LOW;
    }
}
