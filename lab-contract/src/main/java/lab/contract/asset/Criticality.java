package lab.contract.asset;

/** 业务重要性分级，来自资产信息；用于表达业务影响，而非漏洞严重性。 */
public enum Criticality {
    /** 低业务重要性。 */
    LOW,
    /** 中等业务重要性。 */
    MEDIUM,
    /** 高业务重要性，当前规则会据此附加风险分。 */
    HIGH
}
