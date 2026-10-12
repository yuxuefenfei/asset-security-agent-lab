package lab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * 仅监听本机回环地址的教学 HTTP 服务，不作为生产认证或部署方案的参考。
 */
public final class LabServer {
    private final DemoData data = new DemoData();
    private final AgentService agent = new AgentService(data);
    private final TicketService tickets = new TicketService(agent);
    private final String token = System.getenv().getOrDefault("LAB_API_TOKEN", "learn-agent-local");

    public static void main(String[] args) throws Exception {
        new LabServer().start();
    }

    public void start() throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("LAB_PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("[LAB] Started at http://127.0.0.1:" + port);
        System.out.println("[LAB] Test assets: 192.0.2.10, 192.0.2.20; local token from LAB_API_TOKEN");
        System.out.println("[WARNING] In-memory data, fixed-tenant demo, no real IAM. Never expose this port publicly.");
    }

    private void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (path.equals("/") && method.equals("GET")) {
                byte[] page = Objects.requireNonNull(getClass().getResourceAsStream("/index.html")).readAllBytes();
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                ex.getResponseHeaders().set("Cache-Control", "no-store");
                ex.sendResponseHeaders(200, page.length);
                try (OutputStream out = ex.getResponseBody()) {
                    out.write(page);
                }
                return;
            }
            // X-Lab-Token 仅用于本机教学实验，不提供生产级身份认证与访问控制。
            if (!token.equals(ex.getRequestHeaders().getFirst("X-Lab-Token")))
                throw new SecurityException("需要有效的 X-Lab-Token（本地实验口令）");
            Map<String, String> query = queryParams(ex.getRequestURI().getRawQuery());
            Object result;
            if (method.equals("GET") && path.equals("/api/health"))
                result = Json.map("status", "UP", "timestamp", Instant.now().toString(), "demo", true);
            else if (method.equals("GET") && path.equals("/api/assets")) {
                String ip = query.get("ip");
                result = ip == null ? data.ownedAssets() : checkedAsset(ip);
            } else if (method.equals("GET") && path.equals("/api/vulnerabilities")) {
                String ip = query.get("ip");
                new ScopedTools(data, ip);
                result = data.vulnerabilities(ip);
            } else if (method.equals("GET") && path.equals("/api/knowledge")) {
                String q = query.getOrDefault("q", "");
                if (q.length() > 200) throw new IllegalArgumentException("检索内容不能超过200字");
                result = q.isBlank() ? List.of() : data.search(q, 5);
            } else if (method.equals("POST") && path.equals("/api/analyze")) {
                Map<String, Object> body = body(ex);
                result = agent.analyze(Json.str(body.get("ip")), Json.str(body.getOrDefault("question", "分析资产安全风险")), Json.str(body.getOrDefault("mode", "demo")));
            } else if (method.equals("GET") && path.equals("/api/analyses")) result = agent.list();
            else if (method.equals("POST") && path.equals("/api/tickets/draft")) {
                Map<String, Object> body = body(ex);
                result = tickets.draft(Json.str(body.get("analysisId")));
            } else if (method.equals("POST") && path.equals("/api/tickets/approve")) {
                Map<String, Object> body = body(ex);
                result = tickets.approve(Json.str(body.get("ticketId")), Json.str(body.get("approvalCode")));
            } else if (method.equals("GET") && path.equals("/api/tickets")) result = tickets.list();
            else if (method.equals("GET") && path.equals("/api/tools")) result = ScopedTools.schemas();
            else {
                json(ex, 404, Json.map("success", false, "error", "接口不存在"));
                return;
            }
            json(ex, 200, Json.map("success", true, "data", result));
        } catch (SecurityException e) {
            json(ex, 403, Json.map("success", false, "error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            json(ex, 400, Json.map("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("[LAB ERROR] " + e);
            json(ex, 502, Json.map("success", false, "error", "执行失败：" + e.getMessage()));
        }
    }

    private Map<String, Object> checkedAsset(String ip) {
        return Json.object(new ScopedTools(data, ip).call("get_asset", Json.map("ip", ip)));
    }

    private Map<String, Object> body(HttpExchange ex) throws IOException {
        String type = ex.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("application/json"))
            throw new IllegalArgumentException("Content-Type 必须是 application/json");
        byte[] bytes = ex.getRequestBody().readNBytes(16385);
        if (bytes.length > 16384) throw new IllegalArgumentException("请求体过大");
        return Json.object(Json.parse(new String(bytes, StandardCharsets.UTF_8)));
    }

    private Map<String, String> queryParams(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String kv : raw.split("&")) {
            String[] p = kv.split("=", 2);
            m.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8), p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    private void json(HttpExchange ex, int status, Object obj) throws IOException {
        byte[] b = Json.stringify(obj).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(b);
        }
    }
}
