package workshop.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import lab.contract.agent.ToolBackend;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClientException;
import workshop.agent.application.AgentAnalysisService;
import workshop.agent.client.CoreLabGateway;
import workshop.agent.config.*;
import workshop.agent.tool.RequestScopedAssetTools;
import workshop.agent.web.*;

class AgentControllerTest {
    private final CoreLabGateway gateway = mock(CoreLabGateway.class);
    private final ChatModel model = mock(ChatModel.class);
    private final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();

    private MockMvc mvc(ToolBackend backend) {
        when(model.getDefaultOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        var properties =
                new LabProperties(
                        URI.create("http://127.0.0.1:8080"),
                        "token",
                        "192.0.2.10",
                        200,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(10),
                        backend);
        var service =
                new AgentAnalysisService(
                        ChatClient.builder(model).build(),
                        gateway,
                        beans.getBeanProvider(ToolCallbackProvider.class),
                        properties,
                        new AgentPrompt("教学提示词"));
        return MockMvcBuilders.standaloneSetup(new AgentController(service))
                .setControllerAdvice(new AgentExceptionHandler())
                .addFilters(new TraceFilter())
                .build();
    }

    @Test
    void mcpCallbacksReachModel() throws Exception {
        var callbacks = ToolCallbacks.from(new RequestScopedAssetTools(gateway, "192.0.2.10"));
        beans.registerSingleton("mcpTools", ToolCallbackProvider.from(callbacks));
        when(model.call(any(Prompt.class)))
                .thenAnswer(
                        call -> {
                            var options =
                                    (ToolCallingChatOptions)
                                            ((Prompt) call.getArgument(0)).getOptions();
                            assertEquals(3, options.getToolCallbacks().size());
                            assertTrue(
                                    options.getToolCallbacks().stream()
                                            .anyMatch(
                                                    tool ->
                                                            tool.getToolDefinition()
                                                                    .name()
                                                                    .equals("getAsset")));
                            return new ChatResponse(
                                    List.of(new Generation(new AssistantMessage("模拟分析，需要人工复核"))));
                        });
        var response =
                mvc(ToolBackend.MCP)
                        .perform(
                                post("/ai/analyze")
                                        .contentType("application/json")
                                        .content("{\"ip\":\"192.0.2.10\",\"question\":\"分析风险\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();
        assertTrue(response.getContentAsString().contains("\"backend\":\"mcp\""));
        verify(model).call(any(Prompt.class));
    }

    @Test
    void deniesScopeBeforeCallingModel() throws Exception {
        doThrow(new SecurityException("禁止访问")).when(gateway).verifyIp("198.51.100.9");
        mvc(ToolBackend.LOCAL)
                .perform(
                        post("/ai/analyze")
                                .contentType("application/json")
                                .content("{\"ip\":\"198.51.100.9\",\"question\":\"分析\"}"))
                .andExpect(status().isForbidden());
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void rejectsBlankOrLongQuestionBeforeCallingModel() throws Exception {
        for (String question : List.of(" ", "x".repeat(1001)))
            mvc(ToolBackend.LOCAL)
                    .perform(
                            post("/ai/analyze")
                                    .contentType("application/json")
                                    .content(
                                            "{\"ip\":\"192.0.2.10\",\"question\":\""
                                                    + question
                                                    + "\"}"))
                    .andExpect(status().isBadRequest());
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void missingMcpProviderReturnsBadGateway() throws Exception {
        mvc(ToolBackend.MCP)
                .perform(
                        post("/ai/analyze")
                                .contentType("application/json")
                                .content("{\"ip\":\"192.0.2.10\",\"question\":\"分析\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void modelFailureIncludesTraceWithoutDetails() throws Exception {
        when(model.call(any(Prompt.class)))
                .thenThrow(new RestClientException("private upstream details"));
        var response =
                mvc(ToolBackend.LOCAL)
                        .perform(
                                post("/ai/analyze")
                                        .contentType("application/json")
                                        .content("{\"ip\":\"192.0.2.10\",\"question\":\"分析\"}"))
                        .andExpect(status().isBadGateway())
                        .andReturn()
                        .getResponse();
        assertFalse(response.getContentAsString().contains("private upstream details"));
        assertTrue(response.getContentAsString().contains("traceId"));
        assertNotNull(response.getHeader("X-Request-Id"));
    }
}
