package lab.contract.agent;

public record AgentResponse(ToolBackend backend, String assetIp, String answer, String warning) {}
