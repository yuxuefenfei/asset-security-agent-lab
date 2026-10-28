package lab.controller;

import lab.contract.api.ApiResponse;
import lab.contract.dashboard.DashboardResponse;
import lab.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 工作台统计接口，只返回聚合计数，不加载全表到内存计算。 */
@RestController
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping("/api/dashboard")
    public ApiResponse<DashboardResponse> get() {
        return ApiResponse.ok(service.get());
    }
}
