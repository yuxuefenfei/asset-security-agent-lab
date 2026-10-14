package lab;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.*;
import lab.analysis.application.AnalysisService;
import lab.analysis.persistence.AnalysisRepository;
import lab.asset.persistence.AssetMapper;
import lab.contract.analysis.*;
import lab.contract.ticket.*;
import lab.contract.tool.*;
import lab.knowledge.persistence.KnowledgeMapper;
import lab.support.*;
import lab.ticket.application.TicketService;
import lab.tool.application.ToolSessionFactory;
import lab.web.error.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoreIntegrationTest {
    private static final FakeModelServer model = new FakeModelServer();
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired AnalysisService analyses;
    @Autowired AnalysisRepository reports;
    @Autowired TicketService tickets;
    @Autowired ToolSessionFactory sessions;
    @Autowired AssetMapper assets;
    @Autowired KnowledgeMapper knowledge;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", () -> "jdbc:h2:mem:core-tests;DB_CLOSE_DELAY=-1");
        p.add("lab.security.api-token", () -> "test-token");
        p.add("lab.security.approval-code", () -> "test-approval");
        p.add("lab.model.endpoint", model::endpoint);
        p.add("lab.model.max-response-bytes", () -> 32768);
        p.add("lab.model.request-timeout", () -> "800ms");
        p.add("lab.model.total-timeout", () -> "2s");
    }

    @BeforeEach
    void resetModel() {
        model.scenario(FakeModelScenario.FINAL);
    }

    @AfterAll
    static void closeModel() {
        model.close();
    }

    private ResponseEntity<String> request(
            HttpMethod method, String path, Object body, boolean auth) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (auth) headers.set("X-Lab-Token", "test-token");
        return http.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode data(ResponseEntity<String> response) throws Exception {
        assertEquals(200, response.getStatusCode().value(), response.getBody());
        return json.readTree(response.getBody()).get("data");
    }

    private AnalysisReport report(String ip) {
        return analyses.analyze(new AnalysisRequest(ip, "分析", AnalysisMode.DEMO));
    }

    @Test
    void homepageAndProtectedApiContract() throws Exception {
        var homepage = http.getForEntity("/", String.class);
        assertEquals(200, homepage.getStatusCode().value());
        assertTrue(homepage.getBody().contains("id=\"labToken\""));
        assertEquals(
                200, http.getForEntity("/assets/app.js", String.class).getStatusCode().value());
        var denied = request(HttpMethod.GET, "/api/health", null, false);
        assertEquals(403, denied.getStatusCode().value());
        var error = json.readTree(denied.getBody());
        assertFalse(error.path("success").asBoolean());
        assertEquals("FORBIDDEN", error.path("code").asText());
        assertFalse(error.path("traceId").asText().isBlank());
        assertEquals(
                "UP",
                data(request(HttpMethod.GET, "/api/health", null, true)).get("status").asText());
        assertEquals(
                404, request(HttpMethod.GET, "/api/missing", null, true).getStatusCode().value());
    }

    @Test
    void rejectsInvalidTypesUnknownFieldsAndOversizedRequests() {
        for (String body :
                List.of(
                        "{\"ip\":123}",
                        "{\"ip\":\"192.0.2.10\",\"question\":123}",
                        "{\"ip\":\"192.0.2.10\",\"question\":\" \"}",
                        "{\"ip\":\"192.0.2.10\",\"mode\":\"typo\"}",
                        "{\"ip\":\"192.0.2.10\",\"unexpected\":true}",
                        "{\"ip\":\"192.0.2.10\",\"question\":\"" + "x".repeat(1001) + "\"}"))
            assertEquals(
                    400,
                    request(HttpMethod.POST, "/api/analyze", body, true).getStatusCode().value(),
                    body);
        assertEquals(
                400,
                request(HttpMethod.POST, "/api/analyze", "x".repeat(17000), true)
                        .getStatusCode()
                        .value());
        assertEquals(
                400,
                request(HttpMethod.GET, "/api/knowledge?q=" + "x".repeat(201), null, true)
                        .getStatusCode()
                        .value());
        assertEquals(
                403,
                request(HttpMethod.GET, "/api/assets?ip=198.51.100.9", null, true)
                        .getStatusCode()
                        .value());
        assertEquals(
                400,
                request(HttpMethod.GET, "/api/assets?ip=192.0.2.123", null, true)
                        .getStatusCode()
                        .value());
    }

    @Test
    void preservesRiskAndStrictToolScope() throws Exception {
        var first = report("192.0.2.10");
        var second = report("192.0.2.20");
        assertEquals(100, first.riskScore());
        assertEquals(RiskLevel.HIGH, first.riskLevel());
        assertEquals(59, second.riskScore());
        assertEquals(RiskLevel.MEDIUM, second.riskLevel());
        assertEquals(2, first.knowledgeEvidence().size());
        assertEquals(4, first.toolTrace().size());
        var tools = sessions.create("192.0.2.10");
        assertThrows(
                SecurityException.class,
                () ->
                        tools.call(
                                ToolName.GET_ASSET,
                                json.readTree("{\"ip\":\"192.0.2.20\"}"),
                                ToolOrigin.LLM_REQUEST));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.call(
                                ToolName.GET_ASSET,
                                json.readTree("{\"ip\":123}"),
                                ToolOrigin.LLM_REQUEST));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.call(
                                ToolName.GET_ASSET,
                                json.readTree("{\"ip\":\"192.0.2.10\",\"extra\":true}"),
                                ToolOrigin.LLM_REQUEST));
        var schemas = data(request(HttpMethod.GET, "/api/tools", null, true));
        assertEquals(3, schemas.size());
        assertFalse(
                schemas.get(0)
                        .path("function")
                        .path("parameters")
                        .path("additionalProperties")
                        .asBoolean());
        assertEquals(0, data(request(HttpMethod.GET, "/api/knowledge", null, true)).size());
    }

    @Test
    void literalSearchDoesNotTreatUnderscoresAsWildcards() throws Exception {
        assertEquals(0, data(request(HttpMethod.GET, "/api/knowledge?q=__", null, true)).size());
        assertEquals(
                1,
                data(request(HttpMethod.GET, "/api/knowledge?q=lab-2026-0001", null, true)).size());
    }

    @Test
    void historicalSnapshotsSurviveSourceChanges() {
        var original = report("192.0.2.10");
        var asset = assets.selectOneById("ASSET-001");
        var article = knowledge.selectOneById("KB-001");
        String hostname = asset.getHostname(), body = article.getBody();
        try {
            asset.setHostname("changed-host");
            assets.update(asset);
            article.setBody("changed knowledge");
            knowledge.update(article);
            var historical = reports.get(original.id());
            assertEquals(original, historical);
            assertEquals("web-prod-01", historical.asset().hostname());
            assertFalse(
                    historical.knowledgeEvidence().getFirst().body().contains("changed knowledge"));
        } finally {
            asset.setHostname(hostname);
            assets.update(asset);
            article.setBody(body);
            knowledge.update(article);
        }
    }

    @Test
    void draftEditingApprovalVersionAndLocking() throws Exception {
        var report = report("192.0.2.10");
        var draft = tickets.draft(report.id());
        assertEquals(draft, tickets.draft(report.id()));
        assertEquals(
                400,
                request(
                                HttpMethod.POST,
                                "/api/tickets/approve",
                                "{\"ticketId\":\""
                                        + draft.id()
                                        + "\",\"approvalCode\":\"test-approval\"}",
                                true)
                        .getStatusCode()
                        .value());
        assertEquals(
                400,
                request(
                                HttpMethod.POST,
                                "/api/tickets/approve",
                                "{\"ticketId\":\""
                                        + draft.id()
                                        + "\",\"approvalCode\":\"test-approval\",\"version\":0.9}",
                                true)
                        .getStatusCode()
                        .value());
        assertEquals(TicketStatus.DRAFT, tickets.get(draft.id()).status());
        var edited =
                tickets.edit(
                        draft.id(),
                        new EditTicketRequest("人工审核标题", "人工修复方案", "确认维护窗口", draft.version()));
        assertEquals(draft.version() + 1, edited.version());
        var conflict =
                request(
                        HttpMethod.POST,
                        "/api/tickets/approve",
                        new ApproveRequest(draft.id(), "test-approval", draft.version()),
                        true);
        assertEquals(409, conflict.getStatusCode().value());
        assertEquals("VERSION_CONFLICT", json.readTree(conflict.getBody()).path("code").asText());
        assertEquals(
                403,
                request(
                                HttpMethod.POST,
                                "/api/tickets/approve",
                                new ApproveRequest(draft.id(), "wrong", edited.version()),
                                true)
                        .getStatusCode()
                        .value());
        var approved =
                tickets.approve(new ApproveRequest(draft.id(), "test-approval", edited.version()));
        assertEquals(TicketStatus.APPROVED, approved.status());
        assertNotNull(approved.approvedAt());
        assertEquals("人工审核标题", approved.title());
        assertThrows(
                LabException.class,
                () ->
                        tickets.edit(
                                draft.id(),
                                new EditTicketRequest("修改", "方案", "", approved.version())));
        assertEquals(
                400,
                request(
                                HttpMethod.POST,
                                "/api/tickets/approve",
                                new ApproveRequest(draft.id(), "test-approval", approved.version()),
                                true)
                        .getStatusCode()
                        .value());
        assertEquals(approved, tickets.draft(report.id()));
    }

    @Test
    void concurrentDraftsAreUniqueAndOnlyOneEditorWins() throws Exception {
        var report = report("192.0.2.20");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<TicketResponse>>();
            for (int i = 0; i < 8; i++)
                futures.add(executor.submit(() -> tickets.draft(report.id())));
            var ids = new HashSet<String>();
            for (var future : futures) ids.add(future.get(10, TimeUnit.SECONDS).id());
            assertEquals(1, ids.size());
            var draft = tickets.get(ids.iterator().next());
            final var editStart = new CountDownLatch(1);
            var jobs = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 2; i++) {
                int index = i;
                jobs.add(
                        executor.submit(
                                () -> {
                                    editStart.await();
                                    try {
                                        tickets.edit(
                                                draft.id(),
                                                new EditTicketRequest(
                                                        "标题" + index, "方案", "", draft.version()));
                                        return true;
                                    } catch (LabException ex) {
                                        assertEquals(ErrorCode.VERSION_CONFLICT, ex.getCode());
                                        return false;
                                    }
                                }));
            }
            editStart.countDown();
            int winners = 0;
            for (var job : jobs) if (job.get(10, TimeUnit.SECONDS)) winners++;
            assertEquals(1, winners);
            var current = tickets.get(draft.id());
            final var approvalStart = new CountDownLatch(1);
            jobs.clear();
            for (int i = 0; i < 2; i++)
                jobs.add(
                        executor.submit(
                                () -> {
                                    approvalStart.await();
                                    try {
                                        tickets.approve(
                                                new ApproveRequest(
                                                        current.id(),
                                                        "test-approval",
                                                        current.version()));
                                        return true;
                                    } catch (LabException ex) {
                                        assertEquals(ErrorCode.VERSION_CONFLICT, ex.getCode());
                                        return false;
                                    }
                                }));
            approvalStart.countDown();
            winners = 0;
            for (var job : jobs) if (job.get(10, TimeUnit.SECONDS)) winners++;
            assertEquals(1, winners);
        }
    }

    @Test
    void modelUsesRealToolsAndPersistsTheirTrace() {
        model.scenario(FakeModelScenario.TOOLS);
        var report = analyses.analyze(new AnalysisRequest("192.0.2.10", "分析", AnalysisMode.LLM));
        assertEquals(2, model.requests());
        assertTrue(model.returnedTools());
        assertTrue(report.summary().contains("工具调用已完成"));
        assertEquals(
                2,
                report.toolTrace().stream()
                        .filter(trace -> trace.origin() == ToolOrigin.LLM_REQUEST)
                        .count());
        assertEquals(report, reports.get(report.id()));
    }

    @Test
    void modelFailuresDoNotSaveSuccessfulReportsAndRespectBudgets() {
        long before = analyses.page(1, 1).total();
        for (var scenario :
                List.of(
                        FakeModelScenario.HTTP_ERROR,
                        FakeModelScenario.INVALID_JSON,
                        FakeModelScenario.EMPTY_CHOICES,
                        FakeModelScenario.LOOP,
                        FakeModelScenario.OVERSIZED,
                        FakeModelScenario.SLOW_BODY)) {
            model.scenario(scenario);
            long started = System.nanoTime();
            var error =
                    assertThrows(
                            LabException.class,
                            () ->
                                    analyses.analyze(
                                            new AnalysisRequest(
                                                    "192.0.2.10", "分析", AnalysisMode.LLM)));
            assertEquals(ErrorCode.UPSTREAM_FAILURE, error.getCode());
            assertFalse(error.getMessage().contains("private upstream"));
            if (scenario == FakeModelScenario.LOOP) assertEquals(6, model.requests());
            assertTrue(System.nanoTime() - started < TimeUnit.SECONDS.toNanos(4));
        }
        assertEquals(before, analyses.page(1, 1).total());
    }

    @Test
    void paginationIsBoundedAndHasStableMetadata() throws Exception {
        report("192.0.2.10");
        report("192.0.2.20");
        var page = data(request(HttpMethod.GET, "/api/analyses/page?page=1&size=1", null, true));
        assertEquals(1, page.path("items").size());
        assertTrue(page.path("total").asLong() >= 2);
        assertEquals(1, page.path("size").asInt());
        assertEquals(
                400,
                request(HttpMethod.GET, "/api/analyses/page?size=101", null, true)
                        .getStatusCode()
                        .value());
        assertEquals(
                400,
                request(HttpMethod.GET, "/api/analyses/page?page=0", null, true)
                        .getStatusCode()
                        .value());
    }
}
