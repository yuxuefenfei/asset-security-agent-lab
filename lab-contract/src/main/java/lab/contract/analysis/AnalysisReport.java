package lab.contract.analysis;

import java.util.List;
import lab.contract.asset.AssetResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.contract.tool.ToolTrace;
import lab.contract.vulnerability.VulnerabilityResponse;

/**
 * 完整分析报告：资产、漏洞和知识均为分析时快照；评分由 Java 规则决定。 record 用于承载数据并提供同名访问方法，HTTP 响应与持久化对象各司其职。
 *
 * @param id 报告唯一标识，也是工单关联报告时使用的值
 * @param time 报告生成时刻的 ISO-8601 字符串
 * @param mode 本次采用的规则或模型解释模式
 * @param asset 分析时的资产快照
 * @param vulnerabilities 分析时的扫描记录快照，包含不同处理状态
 * @param riskScore 规则计算并封顶的教学分数
 * @param riskLevel 依据分数阈值得到的资产风险等级
 * @param knowledgeEvidence 去重后按首次引用顺序排列的知识正文快照
 * @param summary 规则摘要或模型辅助解释，需要人工核查
 * @param warning 教学资料及使用边界提示
 * @param toolTrace 成功的只读工具调用轨迹
 * @param status 已保存报告的业务状态，当前为分析完成
 */
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
