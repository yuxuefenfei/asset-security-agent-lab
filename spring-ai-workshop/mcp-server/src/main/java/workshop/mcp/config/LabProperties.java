package workshop.mcp.config;

import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * MCP Server 的类型化 lab 配置；本服务只转发授权的只读查询。
 *
 * @param coreUrl 核心 REST 服务地址，由适配层转发查询，不直接访问核心数据库
 * @param apiToken 访问核心 REST 时携带的实验口令
 * @param allowedIp 工具允许访问的固定训练资产 IP
 * @param maxQueryLength 知识搜索参数的字符数上限
 * @param connectTimeout 连接核心服务的时间上限
 * @param requestTimeout 等待核心响应的读取超时
 */
@Validated
@ConfigurationProperties("lab")
public record LabProperties(
        @NotNull URI coreUrl,
        @NotBlank String apiToken,
        @NotBlank String allowedIp,
        @Positive int maxQueryLength,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout) {}
