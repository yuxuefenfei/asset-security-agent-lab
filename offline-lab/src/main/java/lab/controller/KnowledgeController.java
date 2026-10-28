package lab.controller;

import java.util.List;
import lab.config.PolicyProperties;
import lab.contract.api.ApiResponse;
import lab.contract.api.PageResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 知识 HTTP 查询入口；区分工作台分页目录、单条详情和提供给工具的相关性搜索。 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeService service;
    private final PolicyProperties policy;

    @GetMapping("/page")
    public ApiResponse<PageResponse<KnowledgeResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String q) {
        return ApiResponse.ok(service.page(page, size, q));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeResponse>> get(@RequestParam(defaultValue = "") String q) {
        return ApiResponse.ok(service.search(q, policy.apiKnowledgeLimit()));
    }
}
