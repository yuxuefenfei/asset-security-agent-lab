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

@Getter
@Setter
@NoArgsConstructor
@Table("lab_analysis")
public class AnalysisPO {
    @Id(keyType = KeyType.None)
    private String id;

    private String tenantId;
    private String assetIp;
    private Instant createdAt;
    private AnalysisMode mode;
    private Integer riskScore;
    private RiskLevel riskLevel;
    private String summary;
    private String warning;
    private AnalysisStatus status;
    private String assetSnapshot;
    private String vulnerabilitiesSnapshot;
}
