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

/** 手写 Chat Completions 工具循环；只允许经 ToolSession 执行受限的只读能力。 */
@Component
@RequiredArgsConstructor
public class ModelClient {
    private final ModelProperties config;
    private final ObjectMapper json;
    private final ToolSchemas schemas;
    private final HttpClient client;
    private final SystemPrompt prompt;

    /** 执行有轮数、工具次数、总时限及响应容量限制的模型循环；仅返回解释文字，不改变规则分数。 */
    public String reason(String ip, String question, ToolSession tools) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(MessageRole.SYSTEM, prompt.text(), null, null));
            messages.add(
                    new ChatMessage(
                            MessageRole.USER, "分析目标：" + ip + "。用户要求：" + question, null, null));
            // 单调时钟只约束模型阶段；不受系统校时影响，也不包含此前的 Java 预查询。
            long deadline = System.nanoTime() + config.totalTimeout().toNanos();
            int calls = 0;
            for (int round = 0; round < config.maxRounds(); round++) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new IOException("模型分析总时限已到");
                // 每轮同时受单次上限和剩余总预算约束，下载响应体也不能超出此时限。
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
                // 一轮可包含多个工具请求，因此轮数和累计工具次数需要分别限制。
                for (var call : message.toolCalls()) {
                    if (++calls > config.maxToolCalls()
                            || call.id() == null
                            || call.function() == null) throw new IOException("模型工具请求不合法或超过预算");
                    var function = call.function();
                    Object result;
                    try {
                        var args = function.arguments();
                        // 兼容 arguments 为 JSON 字符串的标准形式，再由工具层严格校验结构和范围。
                        if (args != null && args.isTextual())
                            args = json.readTree(args.textValue());
                        result =
                                tools.call(
                                        ToolName.fromCode(function.name()),
                                        args,
                                        ToolOrigin.LLM_REQUEST);
                    } catch (IllegalArgumentException | SecurityException ex) {
                        // 只回传受控拒绝信息，让模型有机会修正调用，不暴露内部异常详情。
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
