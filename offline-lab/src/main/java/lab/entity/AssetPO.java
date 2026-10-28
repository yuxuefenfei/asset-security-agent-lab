package lab.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lab.contract.asset.Criticality;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 资产主表 lab_asset 的持久化对象（PO），描述一台受管理的主机及其业务归属。 数据库以 id 关联端口和漏洞，业务入口通常按 ip 查找；tenantId 决定访问范围。
 * MyBatis-Flex 根据 @Table 和字段名映射表列；Lombok 生成存取方法和映射所需的无参构造器。 @Id 的 KeyType.None
 * 表示主键由数据初始化或调用方提供，框架不自动生成。
 */
@Getter
@Setter
@NoArgsConstructor
@Table("lab_asset")
public class AssetPO {
    /** 资产主键，供端口表和漏洞表引用；与用于展示、查询的 IP 地址不同。 */
    @Id(keyType = KeyType.None)
    private String id;

    /** 资产 IP，是页面和工具查询目标的业务标识；训练数据使用保留的示例地址。 */
    private String ip;

    /** 所属租户标识；查询时必须与服务端配置的当前租户匹配。 */
    private String tenantId;

    /** 主机名称，帮助使用者识别同一 IP 对应的机器用途。 */
    private String hostname;

    /** 操作系统及版本描述，为判断修复方式提供上下文。 */
    private String os;

    /** 承载的业务名称，用于检索和评估整改对业务的影响。 */
    private String business;

    /** 业务重要性；高重要性资产在教学风险规则中获得额外分值。 */
    private Criticality criticality;

    /** 是否对公网暴露；表示已有资产信息，不会触发真实网络探测。 */
    private Boolean internetExposed;

    /** 负责团队或人员的展示名称，便于确定后续整改责任。 */
    private String owner;
}
