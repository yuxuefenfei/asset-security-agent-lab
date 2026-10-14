package lab.contract.api;

/** 固定的接口容量契约；服务可在此上限内通过类型化配置进一步收紧。 */
public final class ApiConstraints {
    public static final int MAX_QUESTION_LENGTH = 1000;
    public static final int MAX_QUERY_LENGTH = 200;
    public static final int MAX_TICKET_TITLE_LENGTH = 200;
    public static final int MAX_REMEDIATION_LENGTH = 4000;
    public static final int MAX_NOTE_LENGTH = 2000;
    public static final int MAX_RISK_SCORE = 100;

    private ApiConstraints() {}
}
