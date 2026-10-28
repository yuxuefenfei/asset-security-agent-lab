package lab.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.Instant;
import lab.contract.analysis.RiskLevel;
import lab.contract.ticket.TicketStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 整改工单当前状态，映射 lab_ticket；同一租户的一份报告只能关联一张工单。 草稿可编辑，审批后锁定；报告提供生成依据，人工复核结果保存在工单中。 */
@Getter
@Setter
@NoArgsConstructor
@Table("lab_ticket")
public class TicketPO {
    /** 工单主键，创建草稿时由应用生成。 */
    @Id(keyType = KeyType.None)
    private String id;

    /** 工单所属租户；必须与读取和修改请求的服务端租户一致。 */
    private String tenantId;

    /** 来源报告的 AnalysisPO.id；唯一约束用于避免重复生成草稿。 */
    private String analysisId;

    /** 整改目标 IP，创建时从报告快照复制，便于展示和检索。 */
    private String assetIp;

    /** 可由人工修改的工单标题，审批后不再允许编辑。 */
    private String title;

    /** 工单状态：草稿待审批，或已经人工审批。 */
    private TicketStatus status;

    /** 创建草稿时从报告复制的风险等级，辅助确定处理优先级。 */
    private RiskLevel riskLevel;

    /** 草稿首次创建时刻；重复生成返回原工单，不刷新此时间。 */
    private Instant createdAt;

    /** 审批人展示名，由服务端配置填写；未审批时为空。 */
    private String approvedBy;

    /** 人工审批完成时刻；未审批时为空。 */
    private Instant approvedAt;

    /** 人工审核备注，可为空；与具体整改措施分开存储。 */
    private String note;

    /** 待人工核对和修改的整改方案；初始内容根据报告知识证据生成。 */
    private String remediation;

    /** 乐观锁版本号：新建为 0，成功更新后由 Flex 自增；更新时比较旧值，服务检查受影响行数以发现并发冲突。 */
    @Column(version = true)
    private Long version;
}
