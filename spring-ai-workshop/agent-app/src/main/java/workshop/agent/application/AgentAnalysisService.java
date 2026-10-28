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

/**
 * Spring AI 分析用例：预先校验目标，再按配置选择本地工具或 MCP 回调，返回模型解释。 它不经过核心 AnalysisService，因此不会保存核心评分报告，也不会创建或审批工单。
 */
@Service
@RequiredArgsConstructor
public class AgentAnalysisService {
    private final ChatClient chatClient;
    private final CoreLabGateway gateway;
    private final ObjectProvider<ToolCallbackProvider> providers;
    private final LabProperties config;
    private final AgentPrompt prompt;

    /** 先校验固定目标，再让 Spring AI 执行模型与工具循环；MCP 回调未连接时直接报错。 */
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
