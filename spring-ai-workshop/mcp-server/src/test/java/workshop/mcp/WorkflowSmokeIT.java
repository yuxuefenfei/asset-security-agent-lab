package workshop.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Maven verify 在全部应用打包后运行真实 core + Agent + MCP，两种模型工具链均使用 fake OpenAI-compatible Chat Completions。
 */
class WorkflowSmokeIT {
    @Test
    void packagedServicesUseTypedCoreContractsInBothModes() throws Exception {
        Path root = Path.of(System.getProperty("lab.projectRoot")).toAbsolutePath().normalize();
        ObjectMapper json = new ObjectMapper();
        List<Process> children = new ArrayList<>();
        List<Path> logs = new ArrayList<>();
        AtomicInteger requests = new AtomicInteger(), returned = new AtomicInteger();
        HttpServer model = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        model.createContext(
                "/v1/chat/completions",
                exchange -> {
                    try {
                        var request = json.readTree(exchange.getRequestBody().readAllBytes());
                        requests.incrementAndGet();
                        var message = json.createObjectNode().put("role", "assistant");
                        int tools = 0;
                        boolean asset = false;
                        for (var item : request.path("messages"))
                            if ("tool".equals(item.path("role").asText())) {
                                tools++;
                                asset |= item.path("content").asText().contains("web-prod-01");
                            }
                        if (tools == 0) {
                            message.put("content", "");
                            var calls = message.putArray("tool_calls");
                            for (var tool : request.path("tools")) {
                                String name = tool.path("function").path("name").asText();
                                var call = calls.addObject();
                                call.put("id", "call_" + calls.size()).put("type", "function");
                                var function = call.putObject("function");
                                function.put("name", name);
                                if (name.endsWith("searchKnowledge"))
                                    function.put("arguments", "{\"query\":\"LAB-2026-0001\"}");
                                else function.put("arguments", "{\"ip\":\"192.0.2.10\"}");
                            }
                            assertEquals(3, calls.size());
                        } else {
                            assertEquals(3, tools);
                            assertTrue(asset);
                            returned.addAndGet(tools);
                            message.put("content", "模拟工具调用已完成，需要人工复核。");
                        }
                        var response =
                                json.createObjectNode()
                                        .put("id", "fake-" + requests.get())
                                        .put("object", "chat.completion")
                                        .put("created", 1)
                                        .put("model", "fake");
                        var choice = response.putArray("choices").addObject().put("index", 0);
                        choice.set("message", message);
                        choice.put("finish_reason", tools == 0 ? "tool_calls" : "stop");
                        response.putObject("usage")
                                .put("prompt_tokens", 1)
                                .put("completion_tokens", 1)
                                .put("total_tokens", 2);
                        byte[] bytes = json.writeValueAsBytes(response);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } catch (Throwable ex) {
                        exchange.sendResponseHeaders(500, -1);
                    } finally {
                        exchange.close();
                    }
                });
        model.start();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        try {
            int corePort = port(), mcpPort = port();
            Process core =
                    start(
                            List.of(
                                    java,
                                    "-jar",
                                    root.resolve("offline-lab/target/offline-lab.jar").toString(),
                                    "--server.port=" + corePort,
                                    "--spring.datasource.url=jdbc:h2:mem:workflow",
                                    "--lab.security.api-token=smoke-token"),
                            root,
                            children,
                            logs);
            ready(client, core, "http://127.0.0.1:" + corePort + "/api/health", "smoke-token");
            Process mcp =
                    start(
                            List.of(
                                    java,
                                    "-jar",
                                    root.resolve(
                                                    "spring-ai-workshop/mcp-server/target/mcp-server-1.0.0.jar")
                                            .toString(),
                                    "--server.port=" + mcpPort,
                                    "--lab.core-url=http://127.0.0.1:" + corePort,
                                    "--lab.api-token=smoke-token"),
                            root,
                            children,
                            logs);
            ready(client, mcp, "http://127.0.0.1:" + mcpPort + "/", null);
            for (String backend : List.of("local", "mcp")) {
                int agentPort = port();
                List<String> command =
                        new ArrayList<>(
                                List.of(
                                        java,
                                        "-jar",
                                        root.resolve(
                                                        "spring-ai-workshop/agent-app/target/agent-app-1.0.0.jar")
                                                .toString(),
                                        "--server.port=" + agentPort,
                                        "--lab.core-url=http://127.0.0.1:" + corePort,
                                        "--lab.api-token=smoke-token",
                                        "--spring.ai.openai.base-url=http://127.0.0.1:"
                                                + model.getAddress().getPort(),
                                        "--spring.ai.openai.chat.options.model=fake",
                                        "--spring.ai.openai.api-key=smoke-test-key",
                                        "--lab.tool-backend=" + backend));
                if (backend.equals("mcp"))
                    command.addAll(
                            List.of(
                                    "--spring.profiles.active=mcp",
                                    "--spring.ai.mcp.client.streamable-http.connections.asset-lab.url=http://127.0.0.1:"
                                            + mcpPort));
                Process agent = start(command, root, children, logs);
                String base = "http://127.0.0.1:" + agentPort;
                ready(client, agent, base + "/", null);
                var response =
                        client.send(
                                HttpRequest.newBuilder(URI.create(base + "/ai/analyze"))
                                        .timeout(Duration.ofSeconds(30))
                                        .header("Content-Type", "application/json")
                                        .POST(
                                                HttpRequest.BodyPublishers.ofString(
                                                        "{\"ip\":\"192.0.2.10\",\"question\":\"分析并调用全部工具\"}"))
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());
                var invalid =
                        client.send(
                                HttpRequest.newBuilder(URI.create(base + "/ai/analyze"))
                                        .header("Content-Type", "application/json")
                                        .POST(
                                                HttpRequest.BodyPublishers.ofString(
                                                        "{\"ip\":123,\"question\":\"分析\"}"))
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());
                assertEquals(400, invalid.statusCode(), invalid.body());
                assertEquals(200, response.statusCode(), response.body());
                assertEquals(backend, json.readTree(response.body()).path("backend").asText());
                assertTrue(response.body().contains("模拟工具调用已完成"));
                stop(agent);
            }
            assertEquals(4, requests.get());
            assertEquals(6, returned.get());
        } catch (Throwable ex) {
            for (Path log : logs) System.err.println(Files.readString(log));
            throw ex;
        } finally {
            for (Process process : children.reversed()) stop(process);
            model.stop(0);
        }
    }

    private Process start(List<String> command, Path root, List<Process> children, List<Path> logs)
            throws Exception {
        Path log =
                Files.createTempFile(
                        Files.createDirectories(root.resolve("offline-lab/target")),
                        "workflow-",
                        ".log");
        logs.add(log);
        Process process =
                new ProcessBuilder(command)
                        .directory(root.toFile())
                        .redirectErrorStream(true)
                        .redirectOutput(log.toFile())
                        .start();
        children.add(process);
        return process;
    }

    private int port() throws Exception {
        try (var socket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
            return socket.getLocalPort();
        }
    }

    private void ready(HttpClient client, Process process, String url, String token)
            throws Exception {
        for (int attempt = 0; attempt < 150 && process.isAlive(); attempt++) {
            try {
                var request =
                        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(1));
                if (token != null) request.header("X-Lab-Token", token);
                var response = client.send(request.build(), HttpResponse.BodyHandlers.discarding());
                if (token == null || response.statusCode() == 200) return;
            } catch (java.io.IOException ignored) {
            }
            Thread.sleep(200);
        }
        throw new AssertionError("service did not start: " + url);
    }

    private void stop(Process process) throws Exception {
        if (!process.isAlive()) return;
        process.destroy();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }
}
