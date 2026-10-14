package workshop.agent.tool;

import java.util.List;
import lab.contract.api.ApiResponse;
import lab.contract.asset.AssetResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.contract.vulnerability.VulnerabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import workshop.agent.client.CoreLabGateway;

@RequiredArgsConstructor
public class RequestScopedAssetTools {
    private final CoreLabGateway gateway;
    private final String allowedIp;

    private void check(String ip) {
        if (!allowedIp.equals(ip)) throw new SecurityException("越权调用资产：" + ip);
        gateway.verifyIp(ip);
    }

    @Tool(description = "根据IP查询授权资产的操作系统、端口、业务归属和网络暴露信息")
    public ApiResponse<AssetResponse> getAsset(@ToolParam(description = "当前任务目标资产IP") String ip) {
        check(ip);
        return gateway.getAsset(ip);
    }

    @Tool(description = "查询授权资产上的漏洞扫描记录，记录为模拟数据且未经核实")
    public ApiResponse<List<VulnerabilityResponse>> getVulnerabilities(
            @ToolParam(description = "当前任务目标资产IP") String ip) {
        check(ip);
        return gateway.getVulnerabilities(ip);
    }

    @Tool(description = "按漏洞编号检索共享教学知识库，返回虚构文档来源")
    public ApiResponse<List<KnowledgeResponse>> searchKnowledge(
            @ToolParam(description = "漏洞编号或安全修复关键词") String query) {
        return gateway.searchKnowledge(query);
    }
}
