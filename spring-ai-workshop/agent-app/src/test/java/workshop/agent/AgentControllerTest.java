package workshop.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentControllerTest {
    private final CoreLabGateway gateway = mock(CoreLabGateway.class);
    private final ChatModel model = mock(ChatModel.class);
    private final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();

    private MockMvc mvc(String backend) {
        when(model.getDefaultOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        return MockMvcBuilders.standaloneSetup(new AgentController(ChatClient.builder(model), gateway,
                        beans.getBeanProvider(ToolCallbackProvider.class), backend))
                .setControllerAdvice(new AgentExceptionHandler()).build();
    }

    @Test
    void mcpProviderMakesCallbacksAvailableToModel() throws Exception {
        var callbacks = ToolCallbacks.from(new RequestScopedAssetTools(gateway, "192.0.2.10"));
        beans.registerSingleton("mcpTools", ToolCallbackProvider.from(callbacks));
        when(model.call(any(Prompt.class))).thenAnswer(call -> {
            var options = (ToolCallingChatOptions) ((Prompt) call.getArgument(0)).getOptions();
            assertEquals(3, options.getToolCallbacks().size());
            assertTrue(options.getToolCallbacks().stream()
                    .anyMatch(tool -> tool.getToolDefinition().name().equals("getAsset")));
            return new ChatResponse(List.of(new Generation(new AssistantMessage("模拟分析，需要人工复核"))));
        });
        mvc("mcp").perform(post("/ai/analyze").contentType("application/json")
                .content("{\"ip\":\"192.0.2.10\",\"question\":\"分析风险\"}"))
                .andExpect(status().isOk());
        verify(model).call(any(Prompt.class));
    }

    @Test
    void outOfScopeRequestIsForbiddenBeforeModelCall() throws Exception {
        doThrow(new SecurityException("本实验只允许访问 192.0.2.10"))
                .when(gateway).verifyIp("198.51.100.9");
        mvc("local").perform(post("/ai/analyze").contentType("application/json")
                .content("{\"ip\":\"198.51.100.9\",\"question\":\"分析\"}"))
                .andExpect(status().isForbidden());
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void blankQuestionIsRejectedBeforeModelCall() throws Exception {
        mvc("local").perform(post("/ai/analyze").contentType("application/json")
                .content("{\"ip\":\"192.0.2.10\",\"question\":\" \"}"))
                .andExpect(status().isBadRequest());
        verify(model, never()).call(any(Prompt.class));
    }

    @Test
    void missingMcpProviderReturnsBadGateway() throws Exception {
        mvc("mcp").perform(post("/ai/analyze").contentType("application/json")
                .content("{\"ip\":\"192.0.2.10\",\"question\":\"分析\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void modelFailureReturnsBadGatewayWithoutUpstreamDetails() throws Exception {
        when(model.call(any(Prompt.class))).thenThrow(new RestClientException("private upstream details"));
        var response = mvc("local").perform(post("/ai/analyze").contentType("application/json")
                .content("{\"ip\":\"192.0.2.10\",\"question\":\"分析\"}"))
                .andExpect(status().isBadGateway()).andReturn().getResponse();
        assertFalse(response.getContentAsString().contains("private upstream details"));
    }
}
