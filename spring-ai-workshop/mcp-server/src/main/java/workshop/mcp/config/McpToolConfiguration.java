package workshop.mcp.config;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.*;
import workshop.mcp.tool.McpAssetTools;

@Configuration
public class McpToolConfiguration {
    @Bean
    ToolCallbackProvider assetMcpTools(McpAssetTools tools) {
        // 将 @Tool 方法转换为可发现、可执行的回调，供 MCP Server 注册。
        return ToolCallbackProvider.from(ToolCallbacks.from(tools));
    }
}
