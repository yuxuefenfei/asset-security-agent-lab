package workshop.agent.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;

/** 创建 Spring AI ChatClient 和独立系统提示词，保持模型客户端组装与业务编排分离。 */
@Configuration
public class AgentConfiguration {
    /** 使用自动配置的模型 Builder 创建客户端；实际工具在每个分析请求中选择。 */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /** 按 UTF-8 读取独立 Agent 提示文件，便于和核心提示分别调整。 */
    @Bean
    AgentPrompt agentPrompt() throws IOException {
        try (var input = new ClassPathResource("prompts/agent-system.txt").getInputStream()) {
            return new AgentPrompt(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
