package lab.contract.api;

/**
 * 独立 Spring Agent 使用的错误体；该服务没有套用核心 REST 的响应信封。
 *
 * @param error 可对外展示的错误说明
 * @param code 机器可读错误码
 * @param traceId 用于关联日志的请求追踪编号
 */
public record ErrorResponse(String error, String code, String traceId) {}
