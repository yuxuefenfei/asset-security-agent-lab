package workshop.agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Per-request tool object binds the LLM to an explicitly authorized asset. */
public class RequestScopedAssetTools {
    private final CoreLabGateway gateway;
    private final String allowedIp;
    public RequestScopedAssetTools(CoreLabGateway gateway,String allowedIp){this.gateway=gateway;this.allowedIp=allowedIp;}
    private void check(String ip){if(!allowedIp.equals(ip))throw new SecurityException("越权调用资产："+ip);gateway.verifyIp(ip);}
    @Tool(description="根据IP查询授权资产的操作系统、端口、业务归属和网络暴露信息")
    public Object getAsset(@ToolParam(description="当前任务目标资产IP") String ip){check(ip);return gateway.getAsset(ip);}
    @Tool(description="查询授权资产上的漏洞扫描记录，记录为模拟数据且未经核实")
    public Object getVulnerabilities(@ToolParam(description="当前任务目标资产IP") String ip){check(ip);return gateway.getVulnerabilities(ip);}
    @Tool(description="按漏洞编号检索企业内部修复知识库，返回文档来源")
    public Object searchKnowledge(@ToolParam(description="漏洞编号或安全修复关键词") String query){return gateway.searchKnowledge(query);}
}
