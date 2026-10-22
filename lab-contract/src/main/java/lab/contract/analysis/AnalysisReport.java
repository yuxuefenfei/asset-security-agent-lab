package lab.contract.analysis;

import java.util.List;
import lab.contract.asset.AssetResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.contract.tool.ToolTrace;
import lab.contract.vulnerability.VulnerabilityResponse;

/** 分析结果契约：资产、漏洞和知识均为分析时快照；summary 是辅助说明，评分由 Java 规则决定。 */
public record AnalysisReport(
        String id,
        String time,
        AnalysisMode mode,
        AssetResponse asset,
        List<VulnerabilityResponse> vulnerabilities,
        int riskScore,
        RiskLevel riskLevel,
        List<KnowledgeResponse> knowledgeEvidence,
        String summary,
        String warning,
        List<ToolTrace> toolTrace,
        AnalysisStatus status) {}
