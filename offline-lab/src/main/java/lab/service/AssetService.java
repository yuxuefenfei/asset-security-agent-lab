package lab.service;

import static lab.entity.table.AssetPortTableDef.ASSET_PORT;
import static lab.entity.table.AssetTableDef.ASSET;

import com.mybatisflex.core.query.QueryWrapper;
import java.util.List;
import lab.config.PolicyProperties;
import lab.config.SecurityProperties;
import lab.contract.api.PageResponse;
import lab.contract.asset.AssetResponse;
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
        pages.validate(number, size);
        var result =
                assets.paginate(
                        number,
                        size,
                        QueryWrapper.create()
                                .where(ASSET.TENANT_ID.eq(security.tenantId()))
                                .orderBy(ASSET.ID.asc()));
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
