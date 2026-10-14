package lab.analysis.persistence;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table("lab_analysis_evidence")
public class AnalysisEvidencePO {
    @Id(keyType = KeyType.None)
    private String id;

    private String analysisId;
    private Integer position;
    private String knowledgeId;
    private String title;
    private String keywords;
    private String body;
    private String source;
}
