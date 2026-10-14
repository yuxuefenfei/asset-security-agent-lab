package lab.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table("lab_asset_port")
public class AssetPortPO {
    @Id(keyType = KeyType.None)
    private String id;

    private String assetId;
    private Integer port;
}
