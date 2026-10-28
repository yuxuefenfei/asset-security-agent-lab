package lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 模型申请执行工具的一次调用；申请本身不会直接访问数据库。
 *
 * @param id 调用唯一编号，用于把执行结果与本次申请配对
 * @param type 上游声明的调用类型，通常为 function
 * @param function 待校验的函数名称与参数
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolCall(String id, String type, ToolCallFunction function) {}
