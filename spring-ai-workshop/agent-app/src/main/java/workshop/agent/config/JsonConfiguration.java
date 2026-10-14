package workshop.agent.config;

import lab.contract.api.JsonContract;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfiguration {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictJsonTypes() {
        return builder -> builder.postConfigurer(JsonContract::configure);
    }
}
