package lab;

import com.sun.net.httpserver.HttpServer;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用确定性的模拟模型，验证 OpenAI 兼容协议中的工具请求、执行与结果回传流程。
 */
public class FakeModelTest {
    public static void main(String[] args) throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                Map<String, Object> in = Json.object(Json.parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                if (!in.containsKey("tools")) throw new AssertionError("tools absent");
                int round = requests.incrementAndGet();
                String response;
                if (round == 1) {
                    response = Json.stringify(Json.map("choices", List.of(Json.map("message", Json.map("role", "assistant", "content", null, "tool_calls", List.of(
                            Json.map("id", "call_1", "type", "function", "function", Json.map("name", "get_asset", "arguments", "{\"ip\":\"192.0.2.10\"}")),
                            Json.map("id", "call_2", "type", "function", "function", Json.map("name", "get_vulnerabilities", "arguments", "{\"ip\":\"192.0.2.10\"}"))
                    ))))));
                } else {
                    List<Object> messages = Json.array(in.get("messages"));
                    if (messages.stream().filter(m -> "tool".equals(Json.object(m).get("role"))).count() != 2)
                        throw new AssertionError("tool results absent");
                    response = Json.stringify(Json.map("choices", List.of(Json.map("message", Json.map("role", "assistant", "content", "工具调用已完成，人工核实后处置。")))));
                }
                byte[] b = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, b.length);
                exchange.getResponseBody().write(b);
            } catch (Throwable t) {
                t.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            ScopedTools tools = new ScopedTools(new DemoData(), "192.0.2.10");
            String text = new ModelToolAgent("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions", "fake", "test-key").reason("192.0.2.10", "测试", tools);
            if (!text.contains("工具调用已完成")) throw new AssertionError("unexpected reply");
            if (tools.trace().size() != 2 || requests.get() != 2)
                throw new AssertionError("expected 2 calls + final answer");
            System.out.println("PASS: Fake LLM two-turn real tool-call protocol test");
        } finally {
            server.stop(0);
        }
    }
}
