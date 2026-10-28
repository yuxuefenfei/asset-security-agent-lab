package lab.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 共享教学知识目录，映射 lab_knowledge，提供解释风险和整改建议的参考正文。 目录没有租户字段；报告引用时会另存正文快照，避免目录变更改写历史依据。 */
@Getter
@Setter
@NoArgsConstructor
@Table("lab_knowledge")
public class KnowledgePO {
    /** 知识条目主键，也是报告证据去重时使用的标识。 */
    @Id(keyType = KeyType.None)
    private String id;

    /** 知识标题，在检索结果和报告证据中展示。 */
    private String title;

    /** 便于匹配的关键词文本，通常包含公告编号或组件名称。 */
    private String keywords;

    /** 知识正文；工具返回实际内容，供模型和人工核对依据。 */
    private String body;

    /** 来源标识；当前 training:// 值是训练资料标记，不是官方公告网址。 */
    private String source;
}
