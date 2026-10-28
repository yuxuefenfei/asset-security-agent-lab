package lab.contract.tool;

/**
 * 提供给模型的工具声明外层，描述能力而不执行工具；执行校验仍由工具会话负责。
 *
 * @param type 工具协议类型，当前固定为 function
 * @param function 函数名称、说明和参数结构
 */
public record ToolDefinition(String type, ToolFunctionDefinition function) {}
