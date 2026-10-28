package lab.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lab.contract.tool.ToolName;
import lab.contract.tool.ToolOrigin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 报告中的成功工具调用明细，映射 lab_tool_trace；按 step 还原执行顺序。 当前轨迹不包含失败调用或模型最终文本，因此不能当作完整审计日志。 */
@Getter
@Setter
@NoArgsConstructor
@Table("lab_tool_trace")
public class ToolTracePO {
    /** 调用轨迹明细主键。 */
    @Id(keyType = KeyType.None)
    private String id;

    /** 所属报告的 AnalysisPO.id。 */
    private String analysisId;

    /** 本次分析内的成功调用序号，从 1 开始；不是模型对话轮数。 */
    private Integer step;

    /** 调用发起方，区分 Java 固定预查询和模型请求。 */
    private ToolOrigin origin;

    /** 实际执行的白名单工具名称。 */
    private ToolName tool;

    /** 调用参数的 JSON 副本，便于核对目标 IP 或检索词。 */
    private String argumentsJson;

    /** 返回记录数；资产对象按 1 条计数，列表按元素数量计数。 */
    private Integer resultCount;
}
