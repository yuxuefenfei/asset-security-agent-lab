package lab;

import java.util.*;

/**
 * 提供三个只读工具；资产访问受固定租户和本次授权 IP 的范围限制。
 */
public final class ScopedTools {
    private final DemoData data;
    private final String ip;
    private final List<Map<String, Object>> trace = new ArrayList<>();

    public ScopedTools(DemoData data, String ip) {
        this.data = data;
        this.ip = ip;
        Map<String, Object> asset = data.asset(ip);
        if (asset == null) throw new IllegalArgumentException("资产不存在：" + ip);
        if (!DemoData.TENANT.equals(asset.get("tenantId"))) throw new SecurityException("当前租户无权访问该资产");
    }

    public static List<Map<String, Object>> schemas() {
        Map<String, Object> ipSchema = Json.map("type", "object", "properties", Json.map("ip", Json.map("type", "string", "description", "本次分析目标资产IP")), "required", List.of("ip"), "additionalProperties", false);
        Map<String, Object> qSchema = Json.map("type", "object", "properties", Json.map("query", Json.map("type", "string", "description", "漏洞编号或漏洞修复知识关键词")), "required", List.of("query"), "additionalProperties", false);
        return List.of(
                Json.map("type", "function", "function", Json.map("name", "get_asset", "description", "查询目标资产基本信息、归属和网络暴露情况", "parameters", ipSchema)),
                Json.map("type", "function", "function", Json.map("name", "get_vulnerabilities", "description", "查询目标资产的漏洞扫描记录", "parameters", ipSchema)),
                Json.map("type", "function", "function", Json.map("name", "search_knowledge", "description", "检索共享的虚构教学知识库，返回来源标识", "parameters", qSchema))
        );
    }

    public List<Map<String, Object>> trace() {
        return List.copyOf(trace);
    }

    public Object call(String name, Map<String, Object> args) {
        return call(name, args, "java-orchestrator");
    }

    public Object call(String name, Map<String, Object> args, String origin) {
        String parameter = switch (name) {
            case "get_asset", "get_vulnerabilities" -> "ip";
            case "search_knowledge" -> "query";
            default -> throw new IllegalArgumentException("工具不在白名单中：" + name);
        };
        if (args == null || !args.keySet().equals(Set.of(parameter)) || !(args.get(parameter) instanceof String))
            throw new IllegalArgumentException("工具参数必须且仅包含字符串字段：" + parameter);
        Object result = switch (name) {
            case "get_asset" -> {
                verifyIp(args);
                yield data.asset(ip);
            }
            case "get_vulnerabilities" -> {
                verifyIp(args);
                yield data.vulnerabilities(ip);
            }
            case "search_knowledge" -> {
                String query = Json.str(args.get("query"));
                if (query.isBlank() || query.length() > 200)
                    throw new IllegalArgumentException("检索内容不能为空或超过200字");
                yield data.search(query, 3);
            }
            default -> throw new IllegalArgumentException("工具不在白名单中：" + name);
        };
        trace.add(Json.map("step", trace.size() + 1, "origin", origin, "tool", name, "arguments", args, "resultCount", result instanceof List<?> l ? l.size() : 1));
        return result;
    }

    private void verifyIp(Map<String, Object> args) {
        if (!ip.equals(Json.str(args.get("ip"))))
            throw new SecurityException("阻止越权工具调用：本次任务只允许查询 " + ip);
    }
}
