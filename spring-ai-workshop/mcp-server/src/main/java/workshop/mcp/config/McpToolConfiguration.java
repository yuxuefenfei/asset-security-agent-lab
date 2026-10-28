package workshop.mcp.config;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.*;
import workshop.mcp.tool.McpAssetTools;

/** 将 @Tool 方法转换为工具回调并注册给 MCP Server，让协议层可以发现声明和执行能力。 */
@Configuration
public class McpToolConfiguration {
    /** 把工具方法的声明和执行入口一起注册，MCP Client 才能发现并远程调用这些能力。 */
    @Bean
    ToolCallbackProvider assetMcpTools(McpAssetTools tools) {
        // 将 @Tool 方法转换为可发现、可执行的回调，供 MCP Server 注册。
        return ToolCallbackProvider.from(ToolCallbacks.from(tools));
    }
}
