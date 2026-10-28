package lab.contract.analysis;

/** 整台资产的风险等级，由规则总分与阈值推导；区别于某条漏洞的严重性。 */
public enum RiskLevel {
    /** 总分低于中风险阈值。 */
    LOW,
    /** 总分达到中风险阈值但未达到高风险阈值。 */
    MEDIUM,
    /** 总分达到高风险阈值。 */
    HIGH
}
