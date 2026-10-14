package lab.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lab.contract.analysis.*;
import lab.contract.api.ApiResponse;
import lab.contract.ticket.*;
import org.junit.jupiter.api.Test;

class ContractTest {
    private final ObjectMapper json = new ObjectMapper();

    ContractTest() {
        lab.contract.api.JsonContract.configure(json);
    }

    @Test
    void preservesExistingEnvelopeAndEnumCodes() throws Exception {
        assertEquals(
                "{\"success\":true,\"data\":[]}",
                json.writeValueAsString(ApiResponse.ok(List.of())));
        var request =
                json.readValue(
                        "{\"ip\":\"192.0.2.10\",\"question\":\"分析\",\"mode\":\"llm\"}",
                        AnalysisRequest.class);
        assertEquals(AnalysisMode.LLM, request.mode());
        assertEquals("\"DRAFT\"", json.writeValueAsString(TicketStatus.DRAFT));
        assertThrows(
                Exception.class,
                () -> json.readValue("{\"ip\":\"x\",\"mode\":\"typo\"}", AnalysisRequest.class));
    }

    @Test
    void scalarFieldsRejectImplicitConversion() {
        assertThrows(
                Exception.class,
                () ->
                        json.readValue(
                                "{\"ip\":123,\"question\":\"分析\"}",
                                lab.contract.agent.AgentRequest.class));
        assertThrows(
                Exception.class,
                () ->
                        json.readValue(
                                "{\"ticketId\":\"id\",\"approvalCode\":\"code\",\"version\":0.9}",
                                ApproveRequest.class));
        assertThrows(
                Exception.class,
                () ->
                        json.readValue(
                                "{\"ticketId\":\"id\",\"approvalCode\":\"code\",\"version\":\"0\"}",
                                ApproveRequest.class));
    }

    @Test
    void failureHasCodeAndTraceWithoutData() throws Exception {
        var error =
                json.readTree(
                        json.writeValueAsString(
                                ApiResponse.failure("失败", "VERSION_CONFLICT", "trace-123")));
        assertFalse(error.has("data"));
        assertEquals("trace-123", error.get("traceId").asText());
        assertEquals("VERSION_CONFLICT", error.get("code").asText());
    }
}
