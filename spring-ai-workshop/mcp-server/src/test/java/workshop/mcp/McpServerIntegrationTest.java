package workshop.mcp;

import com.sun.net.httpserver.HttpServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIntegrationTest {
    private static final AtomicInteger coreCalls = new AtomicInteger();
    private static final HttpServer core = startCore();
    @LocalServerPort
    private int port;

    private static HttpServer startCore() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/", exchange -> {
                coreCalls.incrementAndGet();
                byte[] body = "{\"success\":true,\"data\":{\"ip\":\"192.0.2.10\"}}".getBytes(StandardCharsets.UTF_8);
                boolean authorized = "integration-token".equals(exchange.getRequestHeaders().getFirst("X-Lab-Token"));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(authorized ? 200 : 403, body.length);
                try (var out = exchange.getResponseBody()) { out.write(body); }
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("lab.core-url", () -> "http://127.0.0.1:" + core.getAddress().getPort());
        properties.add("lab.api-token", () -> "integration-token");
    }

    private McpSyncClient client() {
        var transport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + port)
                .endpoint("/mcp").build();
        return McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
    }

    @Test
    void discoversThreeReadOnlyToolsAndForwardsAuthorizedCall() {
        try (var client = client()) {
            client.initialize();
            assertEquals(Set.of("getAsset", "getVulnerabilities", "searchKnowledge"),
                    client.listTools().tools().stream().map(McpSchema.Tool::name).collect(Collectors.toSet()));
            var result = client.callTool(new McpSchema.CallToolRequest("getAsset", Map.of("ip", "192.0.2.10")));
            assertFalse(Boolean.TRUE.equals(result.isError()));
            assertTrue(result.content().toString().contains("192.0.2.10"));
        }
    }

    @Test
    void deniesOutOfScopeAssetWithoutCallingCore() {
        try (var client = client()) {
            client.initialize();
            int before = coreCalls.get();
            var result = client.callTool(new McpSchema.CallToolRequest("getAsset", Map.of("ip", "198.51.100.9")));
            assertTrue(Boolean.TRUE.equals(result.isError()));
            assertEquals(before, coreCalls.get());
        }
    }

    @AfterAll
    static void stopCore() { core.stop(0); }
}
