package lab.controller;

import lab.contract.api.ApiResponse;
import lab.contract.dashboard.DashboardResponse;
import lab.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping("/api/dashboard")
    public ApiResponse<DashboardResponse> get() {
        return ApiResponse.ok(service.get());
    }
}
