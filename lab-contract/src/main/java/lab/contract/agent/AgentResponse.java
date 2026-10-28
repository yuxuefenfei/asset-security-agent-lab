package lab.contract.agent;

/**
 * 独立 Spring Agent 的答复，不代表核心报告已保存，也不包含工单审批结果。
 *
 * @param backend 实际采用的本地方法或 MCP 工具后端
 * @param assetIp 本次解释的目标资产 IP
 * @param answer 模型输出的辅助解释
 * @param warning 提示使用者人工复核的教学说明
 */
public record AgentResponse(ToolBackend backend, String assetIp, String answer, String warning) {}
