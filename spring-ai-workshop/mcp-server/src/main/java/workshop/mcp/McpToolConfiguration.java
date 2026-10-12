package workshop.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfiguration {
    /** The MCP Starter turns the same Spring AI @Tool callbacks into MCP tools/list + tools/call. */
    @Bean
    ToolCallbackProvider assetMcpTools(McpAssetTools tools){
        return ToolCallbackProvider.from(ToolCallbacks.from(tools));
    }
}
