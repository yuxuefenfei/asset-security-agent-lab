package lab.contract.analysis;

/**
 * 报告列表专用摘要；分页只读取必要列，避免为每一行加载完整 JSON 和证据。
 *
 * @param id 报告标识，供打开详情使用
 * @param time 报告生成时刻的 ISO-8601 字符串
 * @param mode 分析执行模式
 * @param assetIp 目标资产 IP
 * @param riskScore 规则风险分数
 * @param riskLevel 资产风险等级
 * @param status 报告业务状态
 */
public record AnalysisSummary(
        String id,
        String time,
        AnalysisMode mode,
        String assetIp,
        int riskScore,
        RiskLevel riskLevel,
        AnalysisStatus status) {}
