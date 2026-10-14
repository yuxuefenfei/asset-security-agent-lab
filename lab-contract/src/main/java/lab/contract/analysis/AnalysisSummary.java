package lab.contract.analysis;

public record AnalysisSummary(
        String id,
        String time,
        AnalysisMode mode,
        String assetIp,
        int riskScore,
        RiskLevel riskLevel,
        AnalysisStatus status) {}
