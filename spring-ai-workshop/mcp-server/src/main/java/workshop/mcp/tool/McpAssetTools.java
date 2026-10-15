package workshop.mcp.tool;

import java.util.List;
import lab.contract.api.ApiResponse;
import lab.contract.asset.AssetResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.contract.vulnerability.VulnerabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import workshop.mcp.config.LabProperties;

@Component
@RequiredArgsConstructor
public class McpAssetTools {
    private final RestClient client;
    private final LabProperties config;

    private void validate(String ip) {
        if (!config.allowedIp().equals(ip))
            throw new SecurityException("MCP 教学工具仅允许访问 " + config.allowedIp());
    }

    @Tool(description = "MCP：查询授权资产基本信息。只读、虚构演示数据")
    public ApiResponse<AssetResponse> getAsset(@ToolParam(description = "资产IP") String ip) {
        validate(ip);
        return client.get()
                .uri(uri -> uri.path("/api/assets").queryParam("ip", ip).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Tool(description = "MCP：查询授权资产漏洞。只读、虚构演示数据")
    public ApiResponse<List<VulnerabilityResponse>> getVulnerabilities(
            @ToolParam(description = "资产IP") String ip) {
        validate(ip);
        return client.get()
                .uri(uri -> uri.path("/api/vulnerabilities").queryParam("ip", ip).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Tool(description = "MCP：按编号查询安全知识库")
    public ApiResponse<List<KnowledgeResponse>> searchKnowledge(
            @ToolParam(description = "漏洞编号或关键词") String query) {
        if (query == null || query.isBlank() || query.length() > config.maxQueryLength())
            throw new IllegalArgumentException("检索词不合法");
        return client.get()
                .uri(uri -> uri.path("/api/knowledge").queryParam("q", query).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
