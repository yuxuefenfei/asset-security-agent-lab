package lab.contract.tool;

import com.fasterxml.jackson.databind.JsonNode;

/** 成功工具调用轨迹；origin 区分 Java 固定预查询与模型请求，不含失败调用或模型最终消息。 */
public record ToolTrace(
        int step, ToolOrigin origin, ToolName tool, JsonNode arguments, int resultCount) {}
