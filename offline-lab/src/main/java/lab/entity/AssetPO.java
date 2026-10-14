package lab.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lab.contract.asset.Criticality;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table("lab_asset")
public class AssetPO {
    @Id(keyType = KeyType.None)
    private String id;

    private String ip;
    private String tenantId;
    private String hostname;
    private String os;
    private String business;
    private Criticality criticality;
    private Boolean internetExposed;
    private String owner;
}
