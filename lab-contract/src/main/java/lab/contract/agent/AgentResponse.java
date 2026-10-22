package lab.contract.agent;

/** 独立 Spring Agent 的答复，不代表核心报告已保存，也不包含工单审批结果。 */
public record AgentResponse(ToolBackend backend, String assetIp, String answer, String warning) {}
