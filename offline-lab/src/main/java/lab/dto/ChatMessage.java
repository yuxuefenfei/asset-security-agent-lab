package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lab.enums.MessageRole;

/**
 * 多轮模型对话中的一条消息；字段按角色组合使用，工具结果通过调用编号关联。
 *
 * @param role 消息来源角色，序列化为上游协议的小写字符串
 * @param content 文字内容或工具结果的 JSON 文本，模型请求工具时可为空
 * @param toolCalls 模型要求执行的工具列表，对应协议中的 tool_calls
 * @param toolCallId 工具结果所回应的调用编号，对应 tool_call_id
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        MessageRole role,
        String content,
        @JsonProperty("tool_calls") List<ToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId) {}
