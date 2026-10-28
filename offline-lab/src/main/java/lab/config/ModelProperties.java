package lab.config;

import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 模型客户端的类型化配置，绑定 lab.model；启动时校验，避免在每次调用中散落默认值。 连接超时、单轮超时和模型阶段总时限控制不同范围，不能互相替代。
 *
 * @param endpoint Chat Completions 的完整 HTTP(S) 接口地址
 * @param model 发送给模型服务的模型标识
 * @param apiKey 可选的上游凭据，仅用于请求头，不应输出到日志或前端
 * @param connectTimeout 建立连接的最长等待时间
 * @param requestTimeout 一次请求及响应体下载的时间上限
 * @param totalTimeout 整个模型对话循环的时间预算，不包含此前 Java 预查询
 * @param maxRounds 模型请求轮数上限，防止对话循环不结束
 * @param maxToolCalls 模型阶段累计工具请求数量上限，一轮可能请求多个工具
 * @param maxResponseBytes 单次模型响应的最大字节数，在接收过程中限制
 * @param maxConcurrentAnalyses 核心分析全流程的并发容量，规则模式也占用名额
 * @param temperature 发给模型的采样温度，不改变 Java 规则评分
 */
@Validated
@ConfigurationProperties("lab.model")
public record ModelProperties(
        @NotNull URI endpoint,
        @NotBlank String model,
        String apiKey,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout,
        @NotNull Duration totalTimeout,
        @Positive int maxRounds,
        @Positive int maxToolCalls,
        @Positive int maxResponseBytes,
        @Positive int maxConcurrentAnalyses,
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature) {
    /** 组合校验地址协议与超时的正值约束，配置错误在启动阶段暴露。 */
    @AssertTrue(message = "模型 URL 必须为 HTTP(S)，超时必须为正数")
    public boolean isSettingsValid() {
        return endpoint != null
                && Set.of("http", "https").contains(endpoint.getScheme())
                && connectTimeout != null
                && !connectTimeout.isNegative()
                && !connectTimeout.isZero()
                && requestTimeout != null
                && !requestTimeout.isNegative()
                && !requestTimeout.isZero()
                && totalTimeout != null
                && !totalTimeout.isNegative()
                && !totalTimeout.isZero();
    }
}
