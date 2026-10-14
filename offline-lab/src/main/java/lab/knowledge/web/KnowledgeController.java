package lab.knowledge.web;

import java.util.List;
import lab.config.PolicyProperties;
import lab.contract.api.ApiResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.knowledge.application.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeService service;
    private final PolicyProperties policy;

    @GetMapping
    public ApiResponse<List<KnowledgeResponse>> get(@RequestParam(defaultValue = "") String q) {
        return ApiResponse.ok(service.search(q, policy.apiKnowledgeLimit()));
    }
}
