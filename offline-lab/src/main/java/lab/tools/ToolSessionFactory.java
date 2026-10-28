package lab.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.config.PolicyProperties;
import lab.service.AssetService;
import lab.service.KnowledgeService;
import lab.service.VulnerabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 每次分析创建一个独立工具会话；在短事务内读取授权资产和漏洞，随后释放数据库事务。 */
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
