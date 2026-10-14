package lab.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lab.contract.tool.ToolName;
import lab.contract.tool.ToolOrigin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table("lab_tool_trace")
public class ToolTracePO {
    @Id(keyType = KeyType.None)
    private String id;

    private String analysisId;
    private Integer step;
    private ToolOrigin origin;
    private ToolName tool;
    private String argumentsJson;
    private Integer resultCount;
}
