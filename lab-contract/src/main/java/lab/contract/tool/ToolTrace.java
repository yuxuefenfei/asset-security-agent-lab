package lab.contract.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 一次成功工具调用的对外轨迹；不包含失败调用或模型最终消息。
 *
 * @param step 本次分析内从 1 开始的成功调用序号
 * @param origin 区分 Java 固定预查询和模型发起的请求
 * @param tool 实际执行的白名单工具
 * @param arguments 参数 JSON 的副本，避免后续修改影响历史轨迹
 * @param resultCount 返回数量，单个资产对象按 1 条计算
 */
public record ToolTrace(
        int step, ToolOrigin origin, ToolName tool, JsonNode arguments, int resultCount) {}
