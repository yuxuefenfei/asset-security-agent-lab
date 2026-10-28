package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 上游的一个候选答复，可能是最终说明，也可能是工具调用请求。
 *
 * @param message 模型返回的消息，内容和工具请求由 ModelClient 判断
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatChoice(ChatMessage message) {}
