package lab.contract.api;

/** 固定 HTTP 协议名集中维护；可调业务策略由各服务的类型化配置负责。 */
public final class LabProtocol {
    /** 访问核心 API 时传递实验口令的 HTTP 请求头名称。 */
    public static final String TOKEN_HEADER = "X-Lab-Token";

    /** 跨服务传播请求追踪编号的 HTTP 头名称。 */
    public static final String TRACE_HEADER = "X-Request-Id";

    private LabProtocol() {}
}
