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

@Getter
@Setter
@NoArgsConstructor
@Table("lab_ticket")
public class TicketPO {
    @Id(keyType = KeyType.None)
    private String id;

    private String tenantId;
    private String analysisId;
    private String assetIp;
    private String title;
    private TicketStatus status;
    private RiskLevel riskLevel;
    private Instant createdAt;
    private String approvedBy;
    private Instant approvedAt;
    private String note;
    private String remediation;

    // Flex 在 UPDATE 中比较旧版本并递增，Service 必须检查受影响行数。
    @Column(version = true)
    private Long version;
}
