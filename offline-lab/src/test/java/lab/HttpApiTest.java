package lab;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** 通过独立子 JVM 和仅监听本机回环地址的模拟模型，验证 HTTP 接口契约。 */
public final class HttpApiTest {
    public static void main(String[] args) throws Exception {
        AtomicReference<String> modelBody = new AtomicReference<>("{}");
        AtomicInteger modelStatus = new AtomicInteger(200);
        AtomicInteger modelCalls = new AtomicInteger();
        HttpServer fake = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fake.createContext("/v1/chat/completions", exchange -> {
            modelCalls.incrementAndGet();
            byte[] bytes = modelBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(modelStatus.get(), bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        fake.start();
        int port;
        try (var socket = new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        Path log = Files.createTempFile(Files.createDirectories(Path.of("offline-lab/target")), "lab-http-test-", ".log");
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder = new ProcessBuilder(javaExecutable, "-cp", System.getProperty("java.class.path"), "lab.LabServer");
        builder.environment().put("LAB_PORT", String.valueOf(port));
        builder.environment().put("LAB_API_TOKEN", "http-test-token");
        builder.environment().put("LAB_APPROVAL_CODE", "http-test-approval");
        builder.environment().put("LLM_API_URL", "http://127.0.0.1:" + fake.getAddress().getPort() + "/v1/chat/completions");
        builder.environment().put("LLM_API_KEY", "");
        Process process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        String base = "http://127.0.0.1:" + port;
        try {
            boolean ready = false;
            for (int attempt = 0; attempt < 50 && process.isAlive(); attempt++) {
                try {
                    ready = send(client, base, "/api/health", null, true).statusCode() == 200;
                    if (ready) break;
                } catch (java.io.IOException ignored) { }
                Thread.sleep(100);
            }
            if (!ready) throw new AssertionError("LabServer startup failed: " + Files.readString(log));
            SelfTest.eq(send(client, base, "/api/assets", null, false).statusCode(), 403, "missing token");
            SelfTest.eq(send(client, base, "/api/assets?ip=198.51.100.9", null, true).statusCode(), 403, "tenant HTTP gate");
            SelfTest.eq(send(client, base, "/api/assets?ip=192.0.2.123", null, true).statusCode(), 400, "unknown asset");
            SelfTest.eq(send(client, base, "/api/knowledge", null, true).body(), "{\"success\":true,\"data\":[]}", "empty search");
            SelfTest.eq(send(client, base, "/api/knowledge?q=" + "x".repeat(201), null, true).statusCode(), 400, "query length limit");
            var report = Json.object(Json.object(Json.parse(send(client, base, "/api/analyze",
                    Json.stringify(Json.map("ip", "192.0.2.10")), true).body())).get("data"));
            SelfTest.eq(report.get("riskScore"), 100L, "demo score");
            String draftBody = Json.stringify(Json.map("analysisId", report.get("id")));
            var draft = send(client, base, "/api/tickets/draft", draftBody, true);
            SelfTest.eq(send(client, base, "/api/tickets/draft", draftBody, true).body(), draft.body(), "HTTP draft idempotency");
            String ticketId = Json.str(Json.object(Json.object(Json.parse(draft.body())).get("data")).get("id"));
            SelfTest.eq(send(client, base, "/api/tickets/approve",
                    Json.stringify(Json.map("ticketId", ticketId, "approvalCode", "wrong")), true).statusCode(), 403, "wrong approval");
            String approveBody = Json.stringify(Json.map("ticketId", ticketId, "approvalCode", "http-test-approval"));
            SelfTest.eq(send(client, base, "/api/tickets/approve", approveBody, true).statusCode(), 200, "HTTP approval");
            SelfTest.eq(send(client, base, "/api/tickets/approve", approveBody, true).statusCode(), 400, "repeat HTTP approval");
            String llmBody = Json.stringify(Json.map("ip", "192.0.2.10", "mode", "llm"));
            modelStatus.set(503);
            modelBody.set("private upstream details");
            var failed = send(client, base, "/api/analyze", llmBody, true);
            SelfTest.eq(failed.statusCode(), 502, "upstream HTTP failure");
            if (failed.body().contains("private upstream details")) throw new AssertionError("upstream detail leaked");
            modelStatus.set(200);
            for (String invalid : List.of("invalid JSON", "{\"choices\":[]}")) {
                modelBody.set(invalid);
                SelfTest.eq(send(client, base, "/api/analyze", llmBody, true).statusCode(), 502, "invalid model reply");
            }
            modelBody.set(Json.stringify(Json.map("choices", List.of(Json.map("message", Json.map("role", "assistant", "tool_calls", List.of(
                    Json.map("id", "loop", "function", Json.map("name", "get_asset", "arguments", "{\"ip\":\"192.0.2.10\"}"))
            )))))));
            int before = modelCalls.get();
            SelfTest.eq(send(client, base, "/api/analyze", llmBody, true).statusCode(), 502, "tool loop limit");
            SelfTest.eq(modelCalls.get() - before, 6, "six model request rounds");
            System.out.println("PASS: HTTP auth, validation, scoring, draft/approval, model failure and loop-limit contracts");
        } finally {
            process.destroy();
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            }
            fake.stop(0);

        }
    }

    private static HttpResponse<String> send(HttpClient client, String base, String path, String body, boolean auth) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(15));
        if (auth) request.header("X-Lab-Token", "http-test-token");
        if (body != null) request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
