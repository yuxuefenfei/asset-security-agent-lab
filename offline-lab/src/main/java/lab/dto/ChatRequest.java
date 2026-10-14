package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lab.contract.tool.ToolDefinition;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(
        String model,
        List<ChatMessage> messages,
        List<ToolDefinition> tools,
        @JsonProperty("tool_choice") String toolChoice,
        double temperature) {}
