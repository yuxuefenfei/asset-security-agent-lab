package lab.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class FakeModelServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicReference<FakeModelScenario> scenario =
            new AtomicReference<>(FakeModelScenario.FINAL);
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicBoolean returnedTools = new AtomicBoolean();

    public FakeModelServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(executor);
            server.createContext(
                    "/v1/chat/completions",
                    exchange -> {
                        var mode = scenario.get();
                        int round = requests.incrementAndGet();
                        String request =
                                new String(
                                        exchange.getRequestBody().readAllBytes(),
                                        StandardCharsets.UTF_8);
                        if (round == 2 && mode == FakeModelScenario.TOOLS) {
                            var messages = new ObjectMapper().readTree(request).get("messages");
                            int count = 0;
                            boolean asset = false;
                            for (var message : messages)
                                if ("tool".equals(message.path("role").asText())) {
                                    count++;
                                    asset |=
                                            message.path("content")
                                                    .asText()
                                                    .contains("web-prod-01");
                                }
                            returnedTools.set(count == 2 && asset);
                        }
                        String body =
                                switch (mode) {
                                    case INVALID_JSON -> "invalid JSON";
                                    case EMPTY_CHOICES -> "{\"choices\":[]}";
                                    case HTTP_ERROR -> "private upstream details";
                                    case OVERSIZED -> "x".repeat(70000);
                                    case LOOP -> calls(false);
                                    case TOOLS -> round == 1 ? calls(true) : answer();
                                    default -> answer();
                                };
                        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                        try {
                            exchange.sendResponseHeaders(
                                    mode == FakeModelScenario.HTTP_ERROR ? 503 : 200, bytes.length);
                            if (mode == FakeModelScenario.SLOW_BODY) {
                                exchange.getResponseBody().write(bytes, 0, 1);
                                exchange.getResponseBody().flush();
                                Thread.sleep(1200);
                                exchange.getResponseBody().write(bytes, 1, bytes.length - 1);
                            } else exchange.getResponseBody().write(bytes);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        } catch (java.io.IOException ignored) {
                        } finally {
                            exchange.close();
                        }
                    });
            server.start();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String answer() {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"工具调用已完成，需要人工复核。\"}}]}";
    }

    private String calls(boolean two) {
        String first =
                "{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"get_asset\",\"arguments\":\"{\\\"ip\\\":\\\"192.0.2.10\\\"}\"}}";
        String second =
                "{\"id\":\"call_2\",\"type\":\"function\",\"function\":{\"name\":\"get_vulnerabilities\",\"arguments\":\"{\\\"ip\\\":\\\"192.0.2.10\\\"}\"}}";
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":["
                + first
                + (two ? "," + second : "")
                + "]}}]}";
    }

    public String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
    }

    public void scenario(FakeModelScenario value) {
        scenario.set(value);
        requests.set(0);
        returnedTools.set(false);
    }

    public int requests() {
        return requests.get();
    }

    public boolean returnedTools() {
        return returnedTools.get();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
