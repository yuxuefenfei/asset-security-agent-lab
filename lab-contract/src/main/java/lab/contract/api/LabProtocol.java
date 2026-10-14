package lab.contract.api;

/** 固定 HTTP 协议名集中维护；可调业务策略由各服务的类型化配置负责。 */
public final class LabProtocol {
    public static final String TOKEN_HEADER = "X-Lab-Token";
    public static final String TRACE_HEADER = "X-Request-Id";

    private LabProtocol() {}
}
