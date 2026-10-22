package lab.controller;

import jakarta.validation.Valid;
import java.util.List;
import lab.contract.analysis.RiskLevel;
import lab.contract.api.*;
import lab.contract.ticket.*;
import lab.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService service;

    @PostMapping("/draft")
    public ApiResponse<TicketResponse> draft(@Valid @RequestBody DraftRequest request) {
        return ApiResponse.ok(service.draft(request.analysisId()));
    }

    @PostMapping("/approve")
    public ApiResponse<TicketResponse> approve(@Valid @RequestBody ApproveRequest request) {
        return ApiResponse.ok(service.approve(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TicketResponse> edit(
            @PathVariable String id, @Valid @RequestBody EditTicketRequest request) {
        return ApiResponse.ok(service.edit(id, request));
    }

    @GetMapping
    public ApiResponse<List<TicketResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<TicketResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) String analysisId) {
        return ApiResponse.ok(service.page(page, size, q, status, riskLevel, analysisId));
    }

    @GetMapping("/{id}")
    public ApiResponse<TicketResponse> get(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }
}
