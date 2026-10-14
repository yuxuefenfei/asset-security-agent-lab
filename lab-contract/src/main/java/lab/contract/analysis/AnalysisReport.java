package lab.contract.analysis;

import java.util.List;
import lab.contract.asset.AssetResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.contract.tool.ToolTrace;
import lab.contract.vulnerability.VulnerabilityResponse;

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
