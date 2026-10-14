package workshop.agent.web;

import jakarta.validation.Valid;
import lab.contract.agent.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import workshop.agent.application.AgentAnalysisService;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AgentController {
    private final AgentAnalysisService service;

    @PostMapping("/analyze")
    public AgentResponse analyze(@Valid @RequestBody AgentRequest request) {
        return service.analyze(request);
    }
}
