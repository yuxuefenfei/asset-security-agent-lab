package workshop.agent.client;

import java.util.List;
import lab.contract.api.ApiResponse;
import lab.contract.asset.AssetResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.contract.vulnerability.VulnerabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import workshop.agent.config.LabProperties;

/** 核心 REST 的类型化只读网关；此处固定 IP 白名单与核心租户授权是两层约束。 */
@Component
@RequiredArgsConstructor
public class CoreLabGateway {
    private final RestClient client;
    private final LabProperties config;

    public void verifyIp(String ip) {
        if (!config.allowedIp().equals(ip))
            throw new SecurityException("本实验只允许访问 " + config.allowedIp());
    }

    public ApiResponse<AssetResponse> getAsset(String ip) {
        verifyIp(ip);
        return client.get()
                .uri(uri -> uri.path("/api/assets").queryParam("ip", ip).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public ApiResponse<List<VulnerabilityResponse>> getVulnerabilities(String ip) {
        verifyIp(ip);
        return client.get()
                .uri(uri -> uri.path("/api/vulnerabilities").queryParam("ip", ip).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public ApiResponse<List<KnowledgeResponse>> searchKnowledge(String query) {
        if (query == null || query.isBlank() || query.length() > config.maxQueryLength())
            throw new IllegalArgumentException("查询词不合法");
        return client.get()
                .uri(uri -> uri.path("/api/knowledge").queryParam("q", query).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
