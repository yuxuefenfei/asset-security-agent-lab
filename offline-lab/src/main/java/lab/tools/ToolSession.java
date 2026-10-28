package lab.tools;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import lab.config.PolicyProperties;
import lab.contract.asset.AssetResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.contract.tool.*;
import lab.contract.vulnerability.VulnerabilityResponse;
import lab.service.KnowledgeService;
import lombok.RequiredArgsConstructor;

/** 一次分析专用的只读上下文；资产与漏洞固定，知识按调用结果收集并去重。 */
@RequiredArgsConstructor
public class ToolSession {
    /** 会话创建时已授权的固定资产，后续工具参数不能改变这个目标。 */
    private final AssetResponse asset;

    /** 会话创建时读取的漏洞快照，规则评分和工具返回使用同一份输入。 */
    private final List<VulnerabilityResponse> vulnerabilities;

    private final KnowledgeService knowledge;
    private final PolicyProperties policy;
    private final ObjectMapper json;

    /** 按执行顺序累积的成功调用；失败请求不进入此列表。 */
    private final List<ToolTrace> trace = new ArrayList<>();

    /** 按知识标识去重并保持首次引用顺序，使报告证据不会因重复检索而重复。 */
    private final LinkedHashMap<String, KnowledgeResponse> evidence = new LinkedHashMap<>();

    public AssetResponse asset() {
        return asset;
    }

    public List<VulnerabilityResponse> vulnerabilities() {
        return vulnerabilities;
    }

    /** 返回列表副本，防止调用方增删会话已收集的轨迹。 */
    public List<ToolTrace> trace() {
        return List.copyOf(trace);
    }

    /** 按知识首次使用的顺序输出去重证据，作为报告快照输入。 */
    public List<KnowledgeResponse> evidence() {
        return List.copyOf(evidence.values());
    }

    /** 严格校验参数并执行白名单工具；只允许会话固定资产，成功结果才累积到证据和轨迹。 */
    public JsonNode call(ToolName name, JsonNode args, ToolOrigin origin) {
        String parameter = name == ToolName.SEARCH_KNOWLEDGE ? "query" : "ip";
        // 不依赖模型自觉遵守 Schema：执行前必须恰好包含一个指定的字符串参数。
        if (args == null
                || !args.isObject()
                || args.size() != 1
                || !args.has(parameter)
                || !args.get(parameter).isTextual())
            throw new IllegalArgumentException("工具参数必须且仅包含字符串字段：" + parameter);
        String value = args.get(parameter).textValue();
        Object result;
        if (name == ToolName.SEARCH_KNOWLEDGE) {
            if (value.isBlank() || value.length() > policy.maxQueryLength())
                throw new IllegalArgumentException("检索词不合法");
            var found = knowledge.search(value, policy.knowledgeLimit());
            // 同一知识可能被多个漏洞或模型重复检索，报告只保留首次使用的正文快照。
            found.forEach(document -> evidence.putIfAbsent(document.id(), document));
            result = found;
        } else {
            // 模型传入的 IP 只是工具参数，不能据此改变本次任务已授权的分析目标。
            if (!asset.ip().equals(value))
                throw new SecurityException("阻止越权工具调用：本次任务只允许查询 " + asset.ip());
            result = name == ToolName.GET_ASSET ? asset : vulnerabilities;
        }
        // 仅成功调用走到这里；拒绝和失败不进入当前成功轨迹，因此它不是完整审计日志。
        trace.add(
                new ToolTrace(
                        trace.size() + 1,
                        origin,
                        name,
                        args.deepCopy(),
                        result instanceof List<?> list ? list.size() : 1));
        return json.valueToTree(result);
    }

    public JsonNode ipArguments() {
        return json.createObjectNode().put("ip", asset.ip());
    }

    public JsonNode queryArguments(String query) {
        return json.createObjectNode().put("query", query);
    }
}
