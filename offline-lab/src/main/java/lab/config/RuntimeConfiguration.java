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

@Configuration
public class RuntimeConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    Semaphore analysisCapacity(ModelProperties settings) {
        return new Semaphore(settings.maxConcurrentAnalyses());
    }

    @Bean
    HttpClient modelHttpClient(ModelProperties settings) {
        return HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).build();
    }

    @Bean
    SystemPrompt systemPrompt() throws IOException {
        try (var input = new ClassPathResource("prompts/core-system.txt").getInputStream()) {
            return new SystemPrompt(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
