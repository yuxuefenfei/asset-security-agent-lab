package lab.contract.api;

/**
 * 核心服务存活信息；表示 HTTP 服务可以响应，不证明外部模型服务可用。
 *
 * @param status 当前健康接口返回的服务状态
 * @param timestamp 服务端生成响应的 ISO-8601 时刻
 * @param demo 是否为教学演示服务
 */
public record HealthResponse(String status, String timestamp, boolean demo) {}
