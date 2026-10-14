package lab.tool.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.asset.application.AssetService;
import lab.config.PolicyProperties;
import lab.knowledge.application.KnowledgeService;
import lab.vulnerability.application.VulnerabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ToolSessionFactory {
    private final AssetService assets;
    private final VulnerabilityService vulnerabilities;
    private final KnowledgeService knowledge;
    private final PolicyProperties policy;
    private final ObjectMapper json;

    /** 短事务固定本次资产/漏洞快照，模型网络调用不占用数据库事务。 */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ToolSession create(String ip) {
        return new ToolSession(assets.get(ip), vulnerabilities.get(ip), knowledge, policy, json);
    }
}
