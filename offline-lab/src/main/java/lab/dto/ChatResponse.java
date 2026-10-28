package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 上游模型响应的最小解析结构；忽略本项目不使用的额外字段。
 *
 * @param choices 候选答复列表，当前客户端读取第一项并检查其消息是否存在
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(List<ChatChoice> choices) {}
