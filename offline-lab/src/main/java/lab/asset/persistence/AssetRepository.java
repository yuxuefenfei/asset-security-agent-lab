package lab.asset.persistence;

import static lab.asset.persistence.table.AssetPortTableDef.ASSET_PORT;
import static lab.asset.persistence.table.AssetTableDef.ASSET;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AssetRepository {
    private final AssetMapper assets;
    private final AssetPortMapper ports;

    public AssetPO findOwned(String ip, String tenant) {
        return assets.selectOneByQuery(
                QueryWrapper.create().where(ASSET.IP.eq(ip)).and(ASSET.TENANT_ID.eq(tenant)));
    }

    public boolean exists(String ip) {
        return assets.selectCountByQuery(QueryWrapper.create().where(ASSET.IP.eq(ip))) > 0;
    }

    public List<AssetPO> list(String tenant, int limit) {
        return assets.selectListByQuery(
                QueryWrapper.create()
                        .where(ASSET.TENANT_ID.eq(tenant))
                        .orderBy(ASSET.ID.asc())
                        .limit(limit));
    }

    public Page<AssetPO> page(String tenant, long page, long size) {
        return assets.paginate(
                page,
                size,
                QueryWrapper.create().where(ASSET.TENANT_ID.eq(tenant)).orderBy(ASSET.ID.asc()));
    }

    public List<Integer> ports(String assetId) {
        return ports
                .selectListByQuery(
                        QueryWrapper.create()
                                .where(ASSET_PORT.ASSET_ID.eq(assetId))
                                .orderBy(ASSET_PORT.PORT.asc()))
                .stream()
                .map(AssetPortPO::getPort)
                .toList();
    }
}
