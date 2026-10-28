package lab.contract.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 函数工具的声明内容；参数结构用于指导模型，不能替代服务端实际校验。
 *
 * @param name 模型调用时使用的白名单函数名称
 * @param description 供模型理解工具用途的说明
 * @param parameters 期望的 JSON Schema 参数声明；具体构造及已知限制见 ToolSchemas
 */
public record ToolFunctionDefinition(ToolName name, String description, JsonNode parameters) {}
