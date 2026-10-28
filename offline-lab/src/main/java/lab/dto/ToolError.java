package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 工具参数或访问范围被拒绝时回给模型的受控结果，允许它修正请求。
 *
 * @param error 不包含内部异常堆栈的拒绝说明
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolError(String error) {}
