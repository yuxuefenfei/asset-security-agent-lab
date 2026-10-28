package lab.config;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.Semaphore;
import lab.dto.SystemPrompt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/** 集中创建时钟、分析并发名额、模型 HTTP 客户端和提示词，业务服务通过构造器注入这些依赖。 */
@Configuration
public class RuntimeConfiguration {
    /** 统一以 UTC 时钟产生绝对时间；注入 Clock 让测试能够替换时间来源。 */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** 共享并发名额，防止规则或模型分析任务同时占用过多资源；调用方负责在 finally 中释放。 */
    @Bean
    Semaphore analysisCapacity(ModelProperties settings) {
        return new Semaphore(settings.maxConcurrentAnalyses());
    }

    /** 复用 JDK HTTP 客户端并配置连接超时；每次请求和模型总时限由 ModelClient 单独控制。 */
    @Bean
    HttpClient modelHttpClient(ModelProperties settings) {
        return HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).build();
    }

    /** 从类路径按 UTF-8 加载提示正文；资源缺失时启动失败，避免用空提示继续运行。 */
    @Bean
    SystemPrompt systemPrompt() throws IOException {
        try (var input = new ClassPathResource("prompts/core-system.txt").getInputStream()) {
            return new SystemPrompt(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
