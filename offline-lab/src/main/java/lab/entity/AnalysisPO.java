package lab.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.Instant;
import lab.contract.analysis.AnalysisMode;
import lab.contract.analysis.AnalysisStatus;
import lab.contract.analysis.RiskLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 已完成分析的主记录，映射 lab_analysis；不是后台运行中的任务对象。 结构化分数由 Java 规则计算，模型只提供 summary；保存输入快照使历史报告可以复核。 */
@Getter
@Setter
@NoArgsConstructor
@Table("lab_analysis")
public class AnalysisPO {
    /** 报告主键，由分析流程生成；证据、工具轨迹和工单通过它关联。 */
    @Id(keyType = KeyType.None)
    private String id;

    /** 报告所属租户；历史报告列表和详情都需要按此字段隔离。 */
    private String tenantId;

    /** 分析目标 IP 的冗余列，供列表筛选使用，无需解析资产快照。 */
    private String assetIp;

    /** 报告生成时刻，使用 Instant 表达绝对时间。 */
    private Instant createdAt;

    /** 执行模式：规则演示或带模型解释；两种模式都由规则决定风险分数。 */
    private AnalysisMode mode;

    /** 教学风险总分，按配置的上限封顶；不是模型自由生成的数值。 */
    private Integer riskScore;

    /** 按阈值从 riskScore 推导的资产风险等级。 */
    private RiskLevel riskLevel;

    /** 规则摘要或模型解释；属于待人工核查的说明文字。 */
    private String summary;

    /** 随报告保存的教学提示，使重新打开历史报告时仍能看见使用边界。 */
    private String warning;

    /** 报告业务状态；当前只持久化分析完成的结果。 */
    private AnalysisStatus status;

    /** 分析时资产响应对象的 JSON 快照，读取历史报告时不改用最新资产。 */
    private String assetSnapshot;

    /** 分析时漏洞响应列表的 JSON 快照，保留当时的扫描依据。 */
    private String vulnerabilitiesSnapshot;
}
