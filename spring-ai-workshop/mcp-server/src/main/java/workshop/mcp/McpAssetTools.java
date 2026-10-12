package workshop.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Read-only MCP facade around existing REST APIs, demonstrating reuse without DB duplication.
 */
@Component
public class McpAssetTools {
    private final RestClient client;
    private final String allowedIp;

    public McpAssetTools(@Value("${lab.core-url}") String url,
                         @Value("${lab.api-token}") String token,
                         @Value("${lab.allowed-ip}") String allowedIp) {
        this.client = RestClient.builder().baseUrl(url).defaultHeader("X-Lab-Token", token).build();
        this.allowedIp = allowedIp;
    }

    private void validate(String ip) {
        if (!allowedIp.equals(ip)) throw new SecurityException("MCP 教学工具仅允许访问 " + allowedIp);
    }

    @Tool(description = "MCP：查询授权资产基本信息。只读、虚构演示数据")
    public Object getAsset(@ToolParam(description = "资产IP") String ip) {
        validate(ip);
        return client.get().uri(uri -> uri.path("/api/assets").queryParam("ip", ip).build()).retrieve().body(Map.class);
    }

    @Tool(description = "MCP：查询授权资产漏洞。只读、虚构演示数据")
    public Object getVulnerabilities(@ToolParam(description = "资产IP") String ip) {
        validate(ip);
        return client.get().uri(uri -> uri.path("/api/vulnerabilities").queryParam("ip", ip).build()).retrieve().body(Map.class);
    }

    @Tool(description = "MCP：按编号查询安全知识库")
    public Object searchKnowledge(@ToolParam(description = "漏洞编号或关键词") String query) {
        if (query == null || query.isBlank() || query.length() > 200)
            throw new IllegalArgumentException("检索词不合法");
        return client.get().uri(uri -> uri.path("/api/knowledge").queryParam("q", query).build()).retrieve().body(Map.class);
    }
}
