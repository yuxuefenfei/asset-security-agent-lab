package lab;

import java.util.*;

public final class SelfTest {
    public static void main(String[] args) throws Exception {
        var parsed = Json.object(Json.parse("{\"n\":1,\"v\":[true,null,\"测试\"],\"s\":\"a\\nb\"}"));
        eq(Json.object(Json.parse(Json.stringify(parsed))), parsed, "JSON round trip");
        DemoData data = new DemoData();
        ScopedTools scoped = new ScopedTools(data, "192.0.2.10");
        eq(Json.object(scoped.call("get_asset", Json.map("ip", "192.0.2.10"))).get("hostname"), "web-prod-01", "asset query");
        throwsType(SecurityException.class, () -> scoped.call("get_asset", Json.map("ip", "192.0.2.20")), "asset scope isolation");
        throwsType(SecurityException.class, () -> new ScopedTools(data, "198.51.100.9"), "tenant isolation");
        throwsType(IllegalArgumentException.class, () -> scoped.call("delete_asset", Json.map("ip", "192.0.2.10")), "tool allowlist");
        throwsType(IllegalArgumentException.class, () -> scoped.call("get_asset", Json.map("ip", 123)), "tool argument type");
        throwsType(IllegalArgumentException.class, () -> scoped.call("get_asset", Json.map("ip", "192.0.2.10", "extra", true)), "tool extra argument");
        throwsType(IllegalArgumentException.class, () -> scoped.call("search_knowledge", Json.map("query", " ")), "empty knowledge query");
        throwsType(IllegalArgumentException.class, () -> scoped.call("search_knowledge", Json.map("query", "x".repeat(201))), "oversized knowledge query");
        if (data.search("LAB-2026-0001", 3).isEmpty()) throw new AssertionError("knowledge retrieval");
        AgentService agent = new AgentService(data);
        var report = agent.analyze("192.0.2.10", "分析", "demo");
        if (!"HIGH".equals(report.get("riskLevel"))) throw new AssertionError("risk assessment");
        if (((List<?>) report.get("toolTrace")).size() < 3) throw new AssertionError("tool trace");
        TicketService tickets = new TicketService(agent);
        var a = tickets.draft(Json.str(report.get("id")));
        var b = tickets.draft(Json.str(report.get("id")));
        eq(a.get("id"), b.get("id"), "ticket draft idempotency");
        throwsType(SecurityException.class, () -> tickets.approve(Json.str(a.get("id")), "wrong-code"), "approval authentication");
        eq(tickets.approve(Json.str(a.get("id")), System.getenv().getOrDefault("LAB_APPROVAL_CODE", "local-approval-only")).get("status"), "APPROVED", "approval");
        throwsType(IllegalStateException.class, () -> tickets.approve(Json.str(a.get("id")), System.getenv().getOrDefault("LAB_APPROVAL_CODE", "local-approval-only")), "repeat approval");
        System.out.println("PASS: JSON, asset query, asset scope, tenant scope, allowlist, retrieval, risk, trace, idempotency, approval, tool argument validation");
    }

    static void throwsType(Class<? extends Throwable> type, Checked f, String label) throws Exception {
        try {
            f.call();
            throw new AssertionError("Expected " + type.getSimpleName() + ": " + label);
        } catch (Throwable t) {
            if (!type.isInstance(t)) throw t instanceof Exception e ? e : new RuntimeException(t);
        }
    }

    static void eq(Object a, Object b, String label) {
        if (!Objects.equals(a, b)) throw new AssertionError(label + ": " + a + " != " + b);
    }

    interface Checked {
        void call() throws Exception;
    }
}
