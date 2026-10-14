package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lab.enums.MessageRole;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        MessageRole role,
        String content,
        @JsonProperty("tool_calls") List<ToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId) {}
