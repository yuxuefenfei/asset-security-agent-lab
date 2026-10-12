package lab;

import java.util.*;

/**
 * 所有记录均为虚构的实验数据，IP 使用文档专用的保留地址段。
 */
public final class DemoData {
    public static final String TENANT = "tenant-blue";
    private final Map<String, Map<String, Object>> assets = new LinkedHashMap<>();
    private final Map<String, List<Map<String, Object>>> vulnerabilities = new LinkedHashMap<>();
    private final List<Map<String, Object>> articles = new ArrayList<>();

    public DemoData() {
        assets.put("192.0.2.10", Json.map("id", "ASSET-001", "ip", "192.0.2.10", "tenantId", TENANT, "hostname", "web-prod-01", "os", "Ubuntu Linux", "business", "订单服务（模拟）", "criticality", "HIGH", "internetExposed", true, "owner", "security-team", "ports", List.of(22, 80, 443, 8080)));
        assets.put("192.0.2.20", Json.map("id", "ASSET-002", "ip", "192.0.2.20", "tenantId", TENANT, "hostname", "db-test-02", "os", "Linux", "business", "测试数据库（模拟）", "criticality", "LOW", "internetExposed", false, "owner", "test-team", "ports", List.of(22, 3306)));
        assets.put("198.51.100.9", Json.map("id", "ASSET-OTHER", "ip", "198.51.100.9", "tenantId", "tenant-red", "hostname", "secret-tenant-host", "os", "Linux", "business", "其他租户", "criticality", "HIGH", "internetExposed", true, "owner", "another-team", "ports", List.of(443)));
        vulnerabilities.put("192.0.2.10", List.of(
                Json.map("id", "VULN-101", "advisory", "LAB-2026-0001", "name", "演示用 Web 框架版本缺陷", "severity", "HIGH", "cvss", 8.6, "component", "DemoWeb 1.2", "status", "OPEN", "scanner", "demo-scanner", "lastSeen", "2026-10-01", "verified", false),
                Json.map("id", "VULN-102", "advisory", "LAB-2026-0002", "name", "演示用弱配置风险", "severity", "MEDIUM", "cvss", 5.3, "component", "DemoConfig", "status", "OPEN", "scanner", "demo-scanner", "lastSeen", "2026-09-29", "verified", false)));
        vulnerabilities.put("192.0.2.20", List.of(Json.map("id", "VULN-103", "advisory", "LAB-2026-0003", "name", "演示用数据库补丁缺失", "severity", "MEDIUM", "cvss", 5.9, "component", "DemoDB", "status", "OPEN", "scanner", "demo-scanner", "lastSeen", "2026-10-03", "verified", false)));
        vulnerabilities.put("198.51.100.9", List.of(Json.map("id", "OTHER-1", "advisory", "PRIVATE", "severity", "CRITICAL")));
        articles.add(Json.map("id", "KB-001", "title", "LAB-2026-0001 DemoWeb 修复公告（虚构）", "keywords", "LAB-2026-0001 DemoWeb Web 框架 补丁 版本", "body", "核实实际运行的 DemoWeb 版本；测试兼容后升级到内部维护的已修复版本；整改后重新扫描。不得仅根据端口开放判定漏洞已被利用。", "source", "training://kb/KB-001"));
        articles.add(Json.map("id", "KB-002", "title", "LAB-2026-0002 Web 基线配置（虚构）", "keywords", "LAB-2026-0002 DemoConfig 配置 基线", "body", "核对配置基线、限制管理接口访问、复核暴露路径，整改后进行配置审计。", "source", "training://kb/KB-002"));
        articles.add(Json.map("id", "KB-003", "title", "LAB-2026-0003 数据库补丁规范（虚构）", "keywords", "LAB-2026-0003 DemoDB 数据库 修复", "body", "确认维护窗口和数据库备份，验证测试环境兼容性后应用补丁，完成回归测试与漏洞复测。", "source", "training://kb/KB-003"));
        articles.add(Json.map("id", "KB-004", "title", "安全工单处理规范", "keywords", "安全 工单 审批 风险 处置 修复", "body", "AI 仅辅助生成分析说明；用户通过独立接口创建整改工单草稿并审批。教学平台仅记录模拟审批，不创建外部正式工单；真实整改后必须由扫描器或管理员重新验证。", "source", "training://policy/SEC-001"));
    }

    public Map<String, Object> asset(String ip) {
        return assets.get(ip);
    }

    public List<Map<String, Object>> ownedAssets() {
        return assets.values().stream().filter(a -> TENANT.equals(a.get("tenantId"))).toList();
    }

    public List<Map<String, Object>> vulnerabilities(String ip) {
        return vulnerabilities.getOrDefault(ip, List.of());
    }

    public List<Map<String, Object>> search(String query, int limit) {
        String q = query.toLowerCase(Locale.ROOT);
        String[] terms = q.split("[^\\p{L}\\p{N}_.-]+");
        return articles.stream().map(article -> {
            String text = (article.get("title") + " " + article.get("keywords") + " " + article.get("body")).toLowerCase(Locale.ROOT);
            int score = 0;
            for (String t : terms) {
                if (t.length() > 1 && text.contains(t)) score += t.startsWith("lab-") ? 5 : 1;
            }
            return Map.entry(score, article);
        }).filter(e -> e.getKey() > 0).sorted((a, b) -> Integer.compare(b.getKey(), a.getKey())).limit(limit).map(Map.Entry::getValue).toList();
    }
}
