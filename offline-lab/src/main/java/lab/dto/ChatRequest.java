package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lab.contract.tool.ToolDefinition;

/**
 * 发送到模型服务的 Chat Completions 请求；只负责上游协议，不作为页面业务接口。
 *
 * @param model 上游模型标识
 * @param messages 按顺序累积的系统、用户、模型和工具消息
 * @param tools 允许模型申请调用的只读工具声明
 * @param toolChoice 工具选择策略，当前发送 auto
 * @param temperature 模型采样温度
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(
        String model,
        List<ChatMessage> messages,
        List<ToolDefinition> tools,
        @JsonProperty("tool_choice") String toolChoice,
        double temperature) {}
