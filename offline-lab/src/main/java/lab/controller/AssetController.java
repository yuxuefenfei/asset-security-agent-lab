package lab.controller;

import lab.contract.api.*;
import lab.contract.asset.AssetResponse;
import lab.contract.asset.Criticality;
import lab.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 资产 HTTP 查询入口；兼容旧的列表或单资产接口，新工作台使用独立分页接口。 */
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {
    private final AssetService service;

    @GetMapping
    public ApiResponse<?> get(@RequestParam(required = false) String ip) {
        return ApiResponse.ok(ip == null ? service.list() : service.get(ip));
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<AssetResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Criticality criticality,
            @RequestParam(required = false) Boolean exposed) {
        return ApiResponse.ok(service.page(page, size, q, criticality, exposed));
    }
}
