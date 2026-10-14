package lab.controller;

import lab.contract.api.*;
import lab.contract.asset.AssetResponse;
import lab.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(service.page(page, size));
    }
}
