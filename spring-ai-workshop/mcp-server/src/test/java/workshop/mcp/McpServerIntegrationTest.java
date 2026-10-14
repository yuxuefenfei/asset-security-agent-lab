package workshop.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIntegrationTest {
    private static final AtomicInteger coreCalls = new AtomicInteger();
    private static final HttpServer core = startCore();
    @LocalServerPort
    private int port;

    private static HttpServer startCore() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(
                    "/api/",
                    exchange -> {
                        coreCalls.incrementAndGet();
                        String payload =
                                switch (exchange.getRequestURI().getPath()) {
                                    case "/api/assets" ->
                                            "{\"id\":\"ASSET-001\",\"ip\":\"192.0.2.10\",\"tenantId\":\"tenant-blue\",\"hostname\":\"web-prod-01\",\"os\":\"Linux\",\"business\":\"订单服务\",\"criticality\":\"HIGH\",\"internetExposed\":true,\"owner\":\"team\",\"ports\":[443]}";
                                    case "/api/vulnerabilities" ->
                                            "[{\"id\":\"VULN-101\",\"advisory\":\"LAB-2026-0001\",\"name\":\"模拟漏洞\",\"severity\":\"HIGH\",\"cvss\":8.6,\"component\":\"DemoWeb\",\"status\":\"OPEN\",\"scanner\":\"demo\",\"lastSeen\":\"2026-10-13\",\"verified\":false}]";
                                    case "/api/knowledge" ->
                                            "[{\"id\":\"KB-001\",\"title\":\"修复公告\",\"keywords\":\"LAB-2026-0001\",\"body\":\"人工复核\",\"source\":\"training://kb/KB-001\"}]";
                                    default -> "null";
                                };
                        byte[] body =
                                ("{\"success\":true,\"data\":" + payload + "}")
                                        .getBytes(StandardCharsets.UTF_8);
                        boolean authorized =
                                "integration-token"
                                        .equals(
                                                exchange.getRequestHeaders()
                                                        .getFirst("X-Lab-Token"));
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(authorized ? 200 : 403, body.length);
                        try (var out = exchange.getResponseBody()) {
                            out.write(body);
                        }
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

    @AfterAll
    static void stopCore() {
        core.stop(0);
    }

    private McpSyncClient client() {
        var transport =
                HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + port)
                        .endpoint("/mcp")
                        .build();
        return McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
    }

    @Test
    void discoversThreeReadOnlyToolsAndForwardsAuthorizedCall() {
        try (var client = client()) {
            client.initialize();
            assertEquals(
                    Set.of("getAsset", "getVulnerabilities", "searchKnowledge"),
                    client.listTools().tools().stream()
                            .map(McpSchema.Tool::name)
                            .collect(Collectors.toSet()));
            var result =
                    client.callTool(
                            new McpSchema.CallToolRequest("getAsset", Map.of("ip", "192.0.2.10")));
            assertFalse(Boolean.TRUE.equals(result.isError()));
            assertTrue(result.content().toString().contains("192.0.2.10"));
        }
    }

    @Test
    void deniesOutOfScopeAssetWithoutCallingCore() {
        try (var client = client()) {
            client.initialize();
            int before = coreCalls.get();
            var result =
                    client.callTool(
                            new McpSchema.CallToolRequest(
                                    "getAsset", Map.of("ip", "198.51.100.9")));
            assertTrue(Boolean.TRUE.equals(result.isError()));
            assertEquals(before, coreCalls.get());
        }
    }

    @Test
    void forwardsTypedVulnerabilityAndKnowledgeLists() {
        try (var client = client()) {
            client.initialize();
            var vulnerabilities =
                    client.callTool(
                            new McpSchema.CallToolRequest(
                                    "getVulnerabilities", Map.of("ip", "192.0.2.10")));
            assertFalse(Boolean.TRUE.equals(vulnerabilities.isError()));
            assertTrue(vulnerabilities.content().toString().contains("VULN-101"));
            var knowledge =
                    client.callTool(
                            new McpSchema.CallToolRequest(
                                    "searchKnowledge", Map.of("query", "LAB-2026-0001")));
            assertFalse(Boolean.TRUE.equals(knowledge.isError()));
            assertTrue(knowledge.content().toString().contains("training://kb/KB-001"));
        }
    }
}
