package lab.controller;

import java.time.Clock;
import lab.contract.api.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 核心服务健康查询入口，返回演示标记与时钟时间；仍受实验口令过滤器保护。 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final Clock clock;

    @GetMapping("/api/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.ok(new HealthResponse("UP", clock.instant().toString(), true));
    }
}
