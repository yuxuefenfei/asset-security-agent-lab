package workshop.agent.config;

import lab.contract.api.JsonContract;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 让 Agent 入口复用核心的严格 JSON 标量类型约束，避免两个服务接受不同格式的请求值。 */
@Configuration
public class JsonConfiguration {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictJsonTypes() {
        return builder -> builder.postConfigurer(JsonContract::configure);
    }
}
