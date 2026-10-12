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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Optional packaged-app smoke test: real Spring/MCP services, deterministic fake Ollama. */
public final class WorkshopSmokeTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
        Path agentJar = root.resolve("spring-ai-workshop/agent-app/target/agent-app-1.0.0.jar");
        Path mcpJar = root.resolve("spring-ai-workshop/mcp-server/target/mcp-server-1.0.0.jar");
        if (!Files.isRegularFile(agentJar) || !Files.isRegularFile(mcpJar))
            throw new IllegalArgumentException("请先执行 mvn -f spring-ai-workshop/pom.xml package");
        List<Process> children = new ArrayList<>();
        List<Path> logs = new ArrayList<>();
        AtomicInteger modelRequests = new AtomicInteger();
        AtomicInteger toolResults = new AtomicInteger();
        HttpServer ollama = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ollama.createContext("/api/chat", exchange -> {
            try {
                var request = Json.object(Json.parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                modelRequests.incrementAndGet();
                List<Object> messages = Json.array(request.get("messages"));
                long returned = messages.stream().filter(m -> "tool".equals(Json.object(m).get("role"))).count();
                Map<String, Object> message;
                if (returned == 0) {
                    List<Map<String, Object>> calls = new ArrayList<>();
                    for (Object tool : Json.array(request.get("tools"))) {
                        String name = Json.str(Json.object(Json.object(tool).get("function")).get("name"));
                        boolean search = name.endsWith("searchKnowledge");
                        calls.add(Json.map("function", Json.map("name", name, "arguments",
                                search ? Json.map("query", "LAB-2026-0001") : Json.map("ip", "192.0.2.10"))));
                    }
                    if (calls.size() != 3) throw new AssertionError("expected 3 Spring/MCP tools, got " + calls.size());
                    message = Json.map("role", "assistant", "content", "", "tool_calls", calls);
                } else {
                    if (returned != 3 || messages.stream().noneMatch(m -> Json.str(Json.object(m).get("content")).contains("web-prod-01")))
                        throw new AssertionError("real core tool results absent");
                    toolResults.addAndGet((int) returned);
                    message = Json.map("role", "assistant", "content", "模拟工具调用已完成，需要人工复核。");
                }
                byte[] bytes = Json.stringify(Json.map("model", "fake", "created_at", "2026-10-12T00:00:00Z",
                        "message", message, "done", true, "done_reason", "stop", "prompt_eval_count", 1, "eval_count", 1)).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (Throwable ex) {
                ex.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            } finally { exchange.close(); }
        });
        ollama.start();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        try {
            int corePort = freePort(), mcpPort = freePort();
            ProcessBuilder coreBuilder = new ProcessBuilder(javaExecutable, "-cp", System.getProperty("java.class.path"), "lab.LabServer");
            coreBuilder.environment().put("LAB_PORT", String.valueOf(corePort));
            coreBuilder.environment().put("LAB_API_TOKEN", "workshop-test-token");
            Process core = start(coreBuilder, children, logs);
            waitReady(client, core, "http://127.0.0.1:" + corePort + "/api/health", "workshop-test-token");
            Process mcp = start(new ProcessBuilder(javaExecutable, "-jar", mcpJar.toString(), "--server.port=" + mcpPort,
                    "--lab.core-url=http://127.0.0.1:" + corePort, "--lab.api-token=workshop-test-token"), children, logs);
            waitReady(client, mcp, "http://127.0.0.1:" + mcpPort + "/", null);
            for (String backend : List.of("local", "mcp")) {
                int agentPort = freePort();
                List<String> command = new ArrayList<>(List.of(javaExecutable, "-jar", agentJar.toString(), "--server.port=" + agentPort,
                        "--lab.core-url=http://127.0.0.1:" + corePort, "--lab.api-token=workshop-test-token",
                        "--spring.ai.ollama.base-url=http://127.0.0.1:" + ollama.getAddress().getPort(), "--spring.ai.ollama.chat.options.model=fake"));
                if (backend.equals("mcp")) {
                    command.add("--spring.profiles.active=mcp");
                    command.add("--spring.ai.mcp.client.streamable-http.connections.asset-lab.url=http://127.0.0.1:" + mcpPort);
                }
                Process agent = start(new ProcessBuilder(command), children, logs);
                String base = "http://127.0.0.1:" + agentPort;
                waitReady(client, agent, base + "/", null);
                var response = client.send(HttpRequest.newBuilder(URI.create(base + "/ai/analyze")).timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                                "{\"ip\":\"192.0.2.10\",\"question\":\"分析并调用全部工具\"}")).build(), HttpResponse.BodyHandlers.ofString());
                SelfTest.eq(response.statusCode(), 200, backend + " response: " + response.body());
                SelfTest.eq(Json.object(Json.parse(response.body())).get("backend"), backend, "selected backend");
                if (!response.body().contains("模拟工具调用已完成")) throw new AssertionError("missing fake model answer");
                stop(agent);
            }
            SelfTest.eq(modelRequests.get(), 4, "two model rounds for each backend");
            SelfTest.eq(toolResults.get(), 6, "three returned tool results for each backend");
            System.out.println("PASS: packaged Spring AI local + MCP end-to-end tool loops (fake Ollama, real core)");
        } catch (Throwable ex) {
            for (Path log : logs) System.err.println(Files.readString(log));
            throw ex;
        } finally {
            for (Process process : children.reversed()) stop(process);
            ollama.stop(0);

        }
    }

    private static int freePort() throws Exception {
        try (var socket = new ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))) { return socket.getLocalPort(); }
    }

    private static Process start(ProcessBuilder builder, List<Process> children, List<Path> logs) throws Exception {
        Path log = Files.createTempFile(Files.createDirectories(Path.of("offline-lab/target")), "workshop-smoke-", ".log");
        logs.add(log);
        Process process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        children.add(process);
        return process;
    }

    private static void waitReady(HttpClient client, Process process, String url, String token) throws Exception {
        for (int attempt = 0; attempt < 150 && process.isAlive(); attempt++) {
            try {
                var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2));
                if (token != null) request.header("X-Lab-Token", token);
                client.send(request.build(), HttpResponse.BodyHandlers.discarding());
                return;
            } catch (java.io.IOException ignored) { }
            Thread.sleep(200);
        }
        throw new AssertionError("service did not start: " + url);
    }

    private static void stop(Process process) throws Exception {
        process.destroy();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }
}
