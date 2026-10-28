package lab.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 资产与端口的一对多明细，映射 lab_asset_port；每行保存一个已知开放端口。 端口单独存表，避免在资产主表中拼接字符串；读取资产时按端口号排序组装。 */
@Getter
@Setter
@NoArgsConstructor
@Table("lab_asset_port")
public class AssetPortPO {
    /** 端口明细主键，由初始化数据提供。 */
    @Id(keyType = KeyType.None)
    private String id;

    /** 关联 AssetPO.id；同一资产可以有多条端口记录。 */
    private String assetId;

    /** 端口号，例如 443；仅是训练数据中的资产信息，不表示实时扫描结果。 */
    private Integer port;
}
