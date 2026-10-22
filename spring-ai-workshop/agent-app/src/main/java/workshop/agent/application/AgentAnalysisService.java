package workshop.agent.application;

import lab.contract.agent.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import workshop.agent.client.CoreLabGateway;
import workshop.agent.config.*;
import workshop.agent.tool.RequestScopedAssetTools;

@Service
@RequiredArgsConstructor
public class AgentAnalysisService {
    private final ChatClient chatClient;
    private final CoreLabGateway gateway;
    private final ObjectProvider<ToolCallbackProvider> providers;
    private final LabProperties config;
    private final AgentPrompt prompt;

    public AgentResponse analyze(AgentRequest request) {
        // 在模型调用之前拒绝越权目标；工具执行时网关仍会再次检查固定 IP。
        gateway.verifyIp(request.ip());
        String instruction = "分析资产 " + request.ip() + "。任务：" + request.question();
        String output;
        if (config.toolBackend() == ToolBackend.MCP) {
            // MCP 模式依赖已连接的回调提供方，没有自动降级；local 必须显式配置。
            var provider =
                    providers
                            .orderedStream()
                            .findFirst()
                            .orElseThrow(
                                    () -> new IllegalStateException("MCP 工具未连接，请先启动 MCP Server"));
            output =
                    chatClient
                            .prompt()
                            .system(prompt.text())
                            .user(instruction)
                            // provider 已提供工具定义与执行器，应注册回调而不是扫描 @Tool 方法。
                            .toolCallbacks(provider)
                            .call()
                            .content();
        } else {
            output =
                    chatClient
                            .prompt()
                            .system(prompt.text())
                            .user(instruction)
                            .tools(new RequestScopedAssetTools(gateway, request.ip()))
                            .call()
                            .content();
        }
        // 此服务只演示工具接入，不经过核心 AnalysisService，不保存评分报告或工单。
        return new AgentResponse(
                config.toolBackend(), request.ip(), output == null ? "" : output, "模拟数据，内容必须人工复核");
    }
}
