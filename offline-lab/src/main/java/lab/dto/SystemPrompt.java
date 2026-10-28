package lab.dto;

/**
 * 从资源文件加载的核心系统提示，作为依赖注入，避免业务代码嵌入长文本。
 *
 * @param text 提示词正文，仅约束模型行为，不能代替代码授权校验
 */
public record SystemPrompt(String text) {}
