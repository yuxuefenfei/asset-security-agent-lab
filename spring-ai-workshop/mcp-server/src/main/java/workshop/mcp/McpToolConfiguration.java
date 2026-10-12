package workshop.mcp;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfiguration {
    /**
     * MCP Starter 将 Spring AI 的 @Tool 回调注册为 MCP 工具，通过 tools/list 发现并通过 tools/call 调用。
     */
    @Bean
    ToolCallbackProvider assetMcpTools(McpAssetTools tools) {
        return ToolCallbackProvider.from(ToolCallbacks.from(tools));
    }
}
