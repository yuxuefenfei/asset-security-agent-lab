package lab.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 报告引用的知识快照，映射 lab_analysis_evidence，是 AnalysisPO 的有序明细。 即使知识目录后来修改或删除，报告仍能通过这些副本还原当时使用的依据。 */
@Getter
@Setter
@NoArgsConstructor
@Table("lab_analysis_evidence")
public class AnalysisEvidencePO {
    /** 证据明细主键；同一知识被不同报告引用时各自拥有独立明细。 */
    @Id(keyType = KeyType.None)
    private String id;

    /** 所属报告的 AnalysisPO.id。 */
    private String analysisId;

    /** 报告内的展示顺序，从 1 开始，按知识首次被使用的顺序保存。 */
    private Integer position;

    /** 原知识条目的标识，用于说明出处；正文读取本行快照。 */
    private String knowledgeId;

    /** 引用时的知识标题副本。 */
    private String title;

    /** 引用时的关键词副本。 */
    private String keywords;

    /** 引用时的完整知识正文，是历史结论可以复核的依据。 */
    private String body;

    /** 引用时的来源标识副本。 */
    private String source;
}
