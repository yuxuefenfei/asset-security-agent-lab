package lab.contract.asset;

import java.util.List;

/**
 * 对外返回的资产视图，由资产主表和端口明细组装；不直接暴露数据库实体。
 *
 * @param id 资产主键，用于内部关联端口和漏洞
 * @param ip 页面和工具使用的目标资产地址
 * @param tenantId 资产所属租户，由后端授权查询确定
 * @param hostname 主机名称
 * @param os 操作系统及版本描述
 * @param business 资产承载的业务名称
 * @param criticality 业务重要性，参与规则评分
 * @param internetExposed 是否存在公网暴露信息，不代表执行过实时探测
 * @param owner 资产负责团队或人员
 * @param ports 按端口号排序的已知端口列表
 */
public record AssetResponse(
        String id,
        String ip,
        String tenantId,
        String hostname,
        String os,
        String business,
        Criticality criticality,
        boolean internetExposed,
        String owner,
        List<Integer> ports) {}
