package lab.config;

import jakarta.validation.constraints.*;
import lab.contract.api.ApiConstraints;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 教学评分规则配置，绑定 lab.risk；不是生产风险模型，也不由模型输出改写。
 *
 * @param exposureBonus 公网暴露资产的附加分
 * @param highCriticalityBonus 高业务重要性资产的附加分
 * @param maxRiskScore 总分封顶值，不能超过接口定义的最大分数
 * @param highThreshold 达到该值即为高风险
 * @param mediumThreshold 达到该值且低于高风险阈值时为中风险，否则为低风险
 */
@Validated
@ConfigurationProperties("lab.risk")
public record RiskProperties(
        @PositiveOrZero int exposureBonus,
        @PositiveOrZero int highCriticalityBonus,
        @Positive @Max(ApiConstraints.MAX_RISK_SCORE) int maxRiskScore,
        @Positive int highThreshold,
        @Positive int mediumThreshold) {
    /** 将 0 到 10 的 CVSS 转为百分制量级，再加上资产属性附加分。 */
    public static final int CVSS_SCALE = 10;

    /** 阈值必须有序，保证同一个分数能得到唯一且合理的风险分级。 */
    @AssertTrue(message = "风险阈值必须满足 0 < medium < high <= max")
    public boolean isThresholdsValid() {
        return mediumThreshold < highThreshold && highThreshold <= maxRiskScore;
    }
}
