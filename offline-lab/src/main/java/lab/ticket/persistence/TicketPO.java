package lab.ticket.persistence;

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

    @Column(version = true)
    private Long version;
}
