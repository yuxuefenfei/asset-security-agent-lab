package lab.config;

import lab.contract.api.JsonContract;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 把共享 JSON 类型约束接入 Spring MVC，避免字符串被悄悄转为整数或小数版本被截断。 */
@Configuration
public class JsonConfiguration {
    /** 在 Jackson 创建完成后应用共享类型约束，覆盖 MVC 请求解析所用的映射器。 */
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictJsonTypes() {
        return builder -> builder.postConfigurer(JsonContract::configure);
    }
}
