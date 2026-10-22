package lab.service;

import static lab.entity.table.AssetPortTableDef.ASSET_PORT;
import static lab.entity.table.AssetTableDef.ASSET;

import com.mybatisflex.core.query.QueryWrapper;
import java.util.List;
import lab.config.PolicyProperties;
import lab.config.SecurityProperties;
import lab.contract.api.PageResponse;
import lab.contract.asset.AssetResponse;
import lab.contract.asset.Criticality;
import lab.dao.AssetMapper;
import lab.dao.AssetPortMapper;
import lab.entity.AssetPO;
import lab.entity.AssetPortPO;
import lab.enums.ErrorCode;
import lab.exception.LabException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetMapper assets;
    private final AssetPortMapper ports;
    private final SecurityProperties security;
    private final PolicyProperties policy;
    private final PagePolicy pages;

    public AssetResponse get(String ip) {
        if (ip == null || ip.isBlank()) throw new IllegalArgumentException("请输入目标资产 IP");
        var asset =
                assets.selectOneByQuery(
                        QueryWrapper.create()
                                .where(ASSET.IP.eq(ip))
                                .and(ASSET.TENANT_ID.eq(security.tenantId())));
        // 先按租户读取；只用无租户计数区分“存在但无权访问”和“资产不存在”。
        if (asset == null) {
            if (assets.selectCountByQuery(QueryWrapper.create().where(ASSET.IP.eq(ip))) > 0)
                throw new LabException(ErrorCode.FORBIDDEN, "当前租户无权访问该资产");
            throw new IllegalArgumentException("资产不存在：" + ip);
        }
        return response(asset);
    }

    public List<AssetResponse> list() {
        return assets
                .selectListByQuery(
                        QueryWrapper.create()
                                .where(ASSET.TENANT_ID.eq(security.tenantId()))
                                .orderBy(ASSET.ID.asc())
                                .limit(policy.legacyListLimit()))
                .stream()
                .map(this::response)
                .toList();
    }

    public PageResponse<AssetResponse> page(long number, long size) {
        return page(number, size, null, null, null);
    }

    public PageResponse<AssetResponse> page(
            long number, long size, String q, Criticality criticality, Boolean exposed) {
        pages.validate(number, size);
        String keyword = pages.keyword(q);
        var query = QueryWrapper.create().where(ASSET.TENANT_ID.eq(security.tenantId()));
        if (!keyword.isEmpty())
            query.and(
                    ASSET.IP
                            .like(keyword)
                            .or(ASSET.HOSTNAME.like(keyword))
                            .or(ASSET.BUSINESS.like(keyword))
                            .or(ASSET.OWNER.like(keyword)));
        if (criticality != null) query.and(ASSET.CRITICALITY.eq(criticality));
        if (exposed != null) query.and(ASSET.INTERNET_EXPOSED.eq(exposed));
        // 筛选和分页都在数据库完成，避免加载全表后再按页截取。
        var result = assets.paginate(number, size, query.orderBy(ASSET.ID.asc()));
        return new PageResponse<>(
                result.getRecords().stream().map(this::response).toList(),
                number,
                size,
                result.getTotalRow());
    }

    private AssetResponse response(AssetPO asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getIp(),
                asset.getTenantId(),
                asset.getHostname(),
                asset.getOs(),
                asset.getBusiness(),
                asset.getCriticality(),
                asset.getInternetExposed(),
                asset.getOwner(),
                ports
                        .selectListByQuery(
                                QueryWrapper.create()
                                        .where(ASSET_PORT.ASSET_ID.eq(asset.getId()))
                                        .orderBy(ASSET_PORT.PORT.asc()))
                        .stream()
                        .map(AssetPortPO::getPort)
                        .toList());
    }
}
