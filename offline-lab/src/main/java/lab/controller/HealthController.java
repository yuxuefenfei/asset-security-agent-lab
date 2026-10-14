package lab.controller;

import java.time.Clock;
import lab.contract.api.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final Clock clock;

    @GetMapping("/api/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.ok(new HealthResponse("UP", clock.instant().toString(), true));
    }
}
