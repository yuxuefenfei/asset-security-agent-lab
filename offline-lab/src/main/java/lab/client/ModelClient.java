package lab.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lab.config.ModelProperties;
import lab.contract.tool.*;
import lab.dto.ChatMessage;
import lab.dto.ChatRequest;
import lab.dto.ChatResponse;
import lab.dto.SystemPrompt;
import lab.dto.ToolError;
import lab.enums.ErrorCode;
import lab.enums.MessageRole;
import lab.exception.LabException;
import lab.tools.ToolSchemas;
import lab.tools.ToolSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelClient {
    private final ModelProperties config;
    private final ObjectMapper json;
    private final ToolSchemas schemas;
    private final HttpClient client;
    private final SystemPrompt prompt;

    public String reason(String ip, String question, ToolSession tools) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(MessageRole.SYSTEM, prompt.text(), null, null));
            messages.add(
                    new ChatMessage(
                            MessageRole.USER, "分析目标：" + ip + "。用户要求：" + question, null, null));
            long deadline = System.nanoTime() + config.totalTimeout().toNanos();
            int calls = 0;
            for (int round = 0; round < config.maxRounds(); round++) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new IOException("模型分析总时限已到");
                Duration timeout =
                        Duration.ofNanos(Math.min(remaining, config.requestTimeout().toNanos()));
                var request =
                        HttpRequest.newBuilder(config.endpoint())
                                .timeout(timeout)
                                .header("Content-Type", "application/json")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                json.writeValueAsString(
                                                        new ChatRequest(
                                                                config.model(),
                                                                messages,
                                                                schemas.definitions(),
                                                                "auto",
                                                                config.temperature()))));
                if (config.apiKey() != null && !config.apiKey().isBlank())
                    request.header("Authorization", "Bearer " + config.apiKey());
                // BodySubscriber 在下载期间限制容量，超时同时覆盖整个响应体。
                var pending =
                        client.sendAsync(
                                request.build(),
                                info -> new LimitedBodySubscriber(config.maxResponseBytes()));
                HttpResponse<String> response;
                try {
                    response = pending.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
                } catch (TimeoutException | InterruptedException ex) {
                    pending.cancel(true);
                    throw ex;
                }
                if (response.statusCode() / 100 != 2) throw new IOException("模型返回非成功状态");
                var reply = json.readValue(response.body(), ChatResponse.class);
                if (reply.choices() == null
                        || reply.choices().isEmpty()
                        || reply.choices().getFirst().message() == null)
                    throw new IOException("模型缺少 choices/message");
                var message = reply.choices().getFirst().message();
                if (message.toolCalls() == null || message.toolCalls().isEmpty())
                    return message.content() == null ? "" : message.content();
                messages.add(message);
                for (var call : message.toolCalls()) {
                    if (++calls > config.maxToolCalls()
                            || call.id() == null
                            || call.function() == null) throw new IOException("模型工具请求不合法或超过预算");
                    var function = call.function();
                    Object result;
                    try {
                        var args = function.arguments();
                        if (args != null && args.isTextual())
                            args = json.readTree(args.textValue());
                        result =
                                tools.call(
                                        ToolName.fromCode(function.name()),
                                        args,
                                        ToolOrigin.LLM_REQUEST);
                    } catch (IllegalArgumentException | SecurityException ex) {
                        result = new ToolError("工具请求被拒绝：参数或访问范围不合法");
                    }
                    messages.add(
                            new ChatMessage(
                                    MessageRole.TOOL,
                                    json.writeValueAsString(result),
                                    null,
                                    call.id()));
                }
            }
            throw new IOException("超过最大工具调用轮次");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new LabException(ErrorCode.UPSTREAM_FAILURE, "模型分析失败，请检查模型服务与工具调用响应", ex);
        }
    }
}
