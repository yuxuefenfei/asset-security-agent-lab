package lab.controller;

import jakarta.validation.Valid;
import java.util.List;
import lab.contract.analysis.*;
import lab.contract.api.*;
import lab.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {
    private final AnalysisService service;

    @PostMapping("/analyze")
    public ApiResponse<AnalysisReport> analyze(@Valid @RequestBody AnalysisRequest request) {
        return ApiResponse.ok(service.analyze(request));
    }

    @GetMapping("/analyses")
    public ApiResponse<List<AnalysisReport>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/analyses/page")
    public ApiResponse<PageResponse<AnalysisSummary>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) AnalysisMode mode) {
        return ApiResponse.ok(service.page(page, size, q, riskLevel, mode));
    }

    @GetMapping("/analyses/{id}")
    public ApiResponse<AnalysisReport> get(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }
}
