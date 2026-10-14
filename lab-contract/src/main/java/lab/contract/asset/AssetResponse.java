package lab.contract.asset;

import java.util.List;

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
