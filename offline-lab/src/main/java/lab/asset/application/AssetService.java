package lab.asset.application;

import java.util.List;
import lab.asset.persistence.*;
import lab.config.*;
import lab.contract.api.PageResponse;
import lab.contract.asset.AssetResponse;
import lab.web.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository repository;
    private final SecurityProperties security;
    private final PolicyProperties policy;
    private final PagePolicy pages;

    public AssetResponse get(String ip) {
        if (ip == null || ip.isBlank()) throw new IllegalArgumentException("请输入目标资产 IP");
        var asset = repository.findOwned(ip, security.tenantId());
        if (asset == null) {
            if (repository.exists(ip)) throw new LabException(ErrorCode.FORBIDDEN, "当前租户无权访问该资产");
            throw new IllegalArgumentException("资产不存在：" + ip);
        }
        return response(asset);
    }

    public List<AssetResponse> list() {
        return repository.list(security.tenantId(), policy.legacyListLimit()).stream()
                .map(this::response)
                .toList();
    }

    public PageResponse<AssetResponse> page(long number, long size) {
        pages.validate(number, size);
        var result = repository.page(security.tenantId(), number, size);
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
                repository.ports(asset.getId()));
    }
}
