package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 模型返回的工具函数申请，保留原始结构供执行边界严格校验。
 *
 * @param name 模型提供的函数名，需要转换为白名单 ToolName 后才能执行
 * @param arguments 可能为 JSON 对象或编码为字符串的 JSON；客户端解码后由工具会话检查
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolCallFunction(String name, JsonNode arguments) {}
