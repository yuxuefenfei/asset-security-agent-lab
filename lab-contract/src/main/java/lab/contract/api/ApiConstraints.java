package lab.contract.api;

/** 固定的接口容量契约；服务可在此上限内通过类型化配置进一步收紧。 */
public final class ApiConstraints {
    /** 分析问题的固定最大字符数；服务配置只能在此范围内收紧。 */
    public static final int MAX_QUESTION_LENGTH = 1000;

    /** 目录与知识检索词的最大字符数。 */
    public static final int MAX_QUERY_LENGTH = 200;

    /** 人工编辑工单标题的最大字符数。 */
    public static final int MAX_TICKET_TITLE_LENGTH = 200;

    /** 工单整改方案的最大字符数；生成初稿时也遵守该上限。 */
    public static final int MAX_REMEDIATION_LENGTH = 4000;

    /** 工单人工备注的最大字符数。 */
    public static final int MAX_NOTE_LENGTH = 2000;

    /** 教学风险分数的固定最大值，供配置和跨语言契约共用。 */
    public static final int MAX_RISK_SCORE = 100;

    private ApiConstraints() {}
}
