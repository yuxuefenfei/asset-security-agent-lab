package workshop.agent.config;

/**
 * 独立 Spring Agent 的系统提示，与核心手写工具循环的提示文件分开维护。
 *
 * @param text 从 agent-system.txt 加载的提示正文
 */
public record AgentPrompt(String text) {}
