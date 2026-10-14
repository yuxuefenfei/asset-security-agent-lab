package workshop.agent.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class AgentConfiguration {
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    AgentPrompt agentPrompt() throws IOException {
        try (var input = new ClassPathResource("prompts/agent-system.txt").getInputStream()) {
            return new AgentPrompt(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
