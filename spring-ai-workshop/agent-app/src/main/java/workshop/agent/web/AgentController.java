package workshop.agent.web;

import jakarta.validation.Valid;
import lab.contract.agent.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import workshop.agent.application.AgentAnalysisService;

/** 独立 Agent 的 HTTP 入口，校验输入后调用应用服务；返回解释响应而非核心报告信封。 */
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
