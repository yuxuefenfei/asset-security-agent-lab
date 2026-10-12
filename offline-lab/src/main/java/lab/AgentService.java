package lab;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 确定性风险评分由 Java 计算；模型仅用于辅助生成分析说明。
 */
public final class AgentService {
    private final DemoData data;
    private final Map<String, Map<String, Object>> analyses = new ConcurrentHashMap<>();

    public AgentService(DemoData data) {
        this.data = data;
    }

    public Map<String, Object> analyze(String ip, String question, String mode) throws Exception {
        if (ip == null || ip.isBlank()) throw new IllegalArgumentException("请输入目标资产 IP");
        if (!Set.of("demo", "llm").contains(mode)) throw new IllegalArgumentException("mode 仅支持 demo 或 llm");
        ScopedTools tools = new ScopedTools(data, ip);
        Map<String, Object> asset = Json.object(tools.call("get_asset", Json.map("ip", ip)));
        List<Map<String, Object>> vulns = vulnList(tools.call("get_vulnerabilities", Json.map("ip", ip)));
        List<Map<String, Object>> evidence = new ArrayList<>();
        int highest = 0;
        for (Map<String, Object> vuln : vulns) {
            String advisory = Json.str(vuln.get("advisory"));
            List<Map<String, Object>> docs = knowledgeList(tools.call("search_knowledge", Json.map("query", advisory)));
            for (Map<String, Object> doc : docs)
                if (evidence.stream().noneMatch(e -> e.get("id").equals(doc.get("id")))) evidence.add(doc);
            int cvss = (int) Math.round(((Number) vuln.getOrDefault("cvss", 0)).doubleValue() * 10);
            highest = Math.max(highest, cvss);
        }
        // 此启发式风险评分仅用于教学，未经真实安全场景验证。
        int score = Math.min(100, highest + (Boolean.TRUE.equals(asset.get("internetExposed")) ? 10 : 0) + ("HIGH".equals(asset.get("criticality")) ? 5 : 0));
        String level = score >= 80 ? "HIGH" : score >= 50 ? "MEDIUM" : "LOW";
        String summary = "【离线教学规则】资产" + ip + "有" + vulns.size() + "条模拟扫描记录。当前风险等级" + level + "（示例评分" + score + "/100）；这些扫描记录均未人工验证。请参考关联知识文档，并在正式整改前确认实际版本、暴露面及影响。";
        String warning = "本系统为演示数据，漏洞编号 LAB-* 非真实 CVE。此评分仅用于学习，不可直接用于生产环境决策。";
        if (mode.equals("llm")) {
            String llmText;
            try {
                llmText = new ModelToolAgent().reason(ip, question, tools);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                throw new IOException("模型分析失败，请检查模型服务与工具调用响应", ex);
            }
            summary = llmText.isBlank() ? "模型未输出解释，已保留规则分析。" : llmText;
        }
        String id = UUID.randomUUID().toString();
        Map<String, Object> report = Json.map("id", id, "time", Instant.now().toString(), "mode", mode, "asset", asset, "vulnerabilities", vulns, "riskScore", score, "riskLevel", level, "knowledgeEvidence", evidence, "summary", summary, "warning", warning, "toolTrace", tools.trace(), "status", "ANALYZED");
        analyses.put(id, report);
        return report;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> vulnList(Object v) {
        return (List<Map<String, Object>>) v;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> knowledgeList(Object v) {
        return (List<Map<String, Object>>) v;
    }

    public Map<String, Object> get(String id) {
        Map<String, Object> value = analyses.get(id);
        if (value == null) throw new IllegalArgumentException("分析结果不存在");
        return value;
    }

    public List<Map<String, Object>> list() {
        return analyses.values().stream().sorted((a, b) -> Json.str(b.get("time")).compareTo(Json.str(a.get("time")))).toList();
    }
}
